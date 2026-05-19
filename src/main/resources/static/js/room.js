const chatId = window.__CHAT_ID__;
const currentUsername = window.__CURRENT_USERNAME__;

let stompClient = null;
let currentPage = 0;
let isLoading = false;
let hasMore = true;
let typingTimeout = null;
let replyToMessageId = null;
let allChats = [];
let pendingFiles = [];
let isUploading = false;
let currentMediaList = [];
let currentMediaIndex = 0;

let mediaRecorder = null;
let voiceChunks = [];
let voiceStartTime = 0;
let voiceTimerId = null;
let voiceStream = null;
let voiceCancelled = false;

function getCsrfToken() {
    return document.querySelector('meta[name="_csrf"]')?.content;
}

function getCsrfHeader() {
    return document.querySelector('meta[name="_csrf_header"]')?.content;
}

function formatFileSize(bytes) {
    if (!bytes) return '';
    const sizes = ['Б', 'КБ', 'МБ', 'ГБ'];
    const i = Math.floor(Math.log(bytes) / Math.log(1024));
    return (bytes / Math.pow(1024, i)).toFixed(1) + ' ' + sizes[i];
}

function formatDuration(seconds) {
    if (!seconds && seconds !== 0) return '00:00';
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
}

function getFileIcon(mediaType, filename) {
    if (mediaType === 'IMAGE') return '<i class="bi bi-image-fill"></i>';
    if (mediaType === 'VIDEO') return '<i class="bi bi-camera-reels-fill"></i>';
    if (mediaType === 'AUDIO') return '<i class="bi bi-music-note-beamed"></i>';
    if (mediaType === 'VOICE_MESSAGE') return '<i class="bi bi-mic-fill"></i>';
    const ext = filename?.split('.').pop()?.toLowerCase();
    if (ext === 'pdf') return '<i class="bi bi-file-pdf-fill"></i>';
    if (ext === 'doc' || ext === 'docx') return '<i class="bi bi-file-word-fill"></i>';
    if (ext === 'xls' || ext === 'xlsx') return '<i class="bi bi-file-excel-fill"></i>';
    if (ext === 'ppt' || ext === 'pptx') return '<i class="bi bi-file-ppt-fill"></i>';
    if (ext === 'txt' || ext === 'md') return '<i class="bi bi-file-text-fill"></i>';
    if (ext === 'zip' || ext === 'rar' || ext === '7z' || ext === 'tar' || ext === 'gz') return '<i class="bi bi-file-zip-fill"></i>';
    if (ext === 'java' || ext === 'py' || ext === 'js' || ext === 'ts' || ext === 'html' || ext === 'css' || ext === 'json' || ext === 'xml' || ext === 'c' || ext === 'cpp' || ext === 'h') return '<i class="bi bi-file-code-fill"></i>';
    return '<i class="bi bi-file-earmark-fill"></i>';
}

function connectWebSocket() {
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    stompClient.debug = null;

    stompClient.connect({}, function () {
        stompClient.subscribe('/topic/chat.' + chatId, function (message) {
            const data = JSON.parse(message.body);

            if (data.action === 'DELETE') {
                const el = document.querySelector('.message[data-id="' + data.messageId + '"]');
                if (el) {
                    const bubble = el.querySelector('.message-bubble');
                    if (bubble) {
                        bubble.textContent = 'Сообщение удалено';
                        bubble.classList.add('deleted');
                    }
                    el.classList.add('deleted');
                    // Убираем кнопки и медиа у удалённого сообщения
                    el.querySelectorAll('.message-actions, .message-reply, .message-media, .message-edited')
                        .forEach(node => node.remove());
                }
            } else if (data.action === 'EDIT') {
                const el = document.querySelector('.message[data-id="' + data.message.id + '"]');
                if (el) {
                    const isOut = data.message.author?.username === currentUsername;
                    el.outerHTML = buildMessageHtml(data.message, isOut);
                }
            } else if (data.id) {
                const isOut = data.author?.username === currentUsername;
                appendMessage(data, isOut);
                if (!isOut) {
                    sendReadReceipt(data.id);
                }
            }
        });

        stompClient.subscribe('/user/queue/typing', function (message) {
            const data = JSON.parse(message.body);
            const indicator = document.getElementById('typingIndicator');
            if (data.chatId.toString() === chatId.toString() && data.isTyping) {
                indicator.textContent = '✏️ Печатает...';
            } else {
                indicator.textContent = '';
            }
        });

        stompClient.subscribe('/user/queue/chats_update', function (message) {
            const chat = JSON.parse(message.body);
            updateChatInList(chat);
        });

        stompClient.subscribe('/user/queue/read', function (message) {
            const data = JSON.parse(message.body);
            if (data.chatId.toString() === chatId.toString()) {
                markAllOutgoingRead();
            }
        });
    }, function () {
        setTimeout(connectWebSocket, 5000);
    });
}

function sendReadReceipt(messageId) {
    if (!chatId || !messageId) return;
    if (stompClient && stompClient.connected) {
        stompClient.send('/app/chat.read', {}, JSON.stringify({ chatId: chatId, messageId: messageId }));
    }
}

function markAllOutgoingRead() {
    document.querySelectorAll('.message.outgoing .message-status').forEach(function (el) {
        if (el.textContent === '✓') el.textContent = '✓✓';
    });
}

async function uploadFiles(files) {
    const formData = new FormData();
    for (const file of files) {
        formData.append('files', file);
    }

    try {
        const response = await fetch('/api/chats/media/upload', {
            method: 'POST',
            headers: { [getCsrfHeader()]: getCsrfToken() },
            body: formData
        });

        if (response.ok) {
            return await response.json();
        }
        return null;
    } catch (error) {
        console.error('Upload error:', error);
        return null;
    }
}

async function uploadVoice(blob, durationSec) {
    const formData = new FormData();
    const ext = (blob.type && blob.type.includes('ogg')) ? 'ogg' : 'webm';
    const file = new File([blob], `voice_${Date.now()}.${ext}`, { type: blob.type || 'audio/webm' });
    formData.append('file', file);

    try {
        const response = await fetch('/api/chats/media/upload-voice', {
            method: 'POST',
            headers: { [getCsrfHeader()]: getCsrfToken() },
            body: formData
        });
        if (response.ok) {
            const m = await response.json();
            if (m) m.duration = durationSec;
            return m;
        }
        return null;
    } catch (error) {
        console.error('Voice upload error:', error);
        return null;
    }
}

function addMediaToPreview(file) {
    const reader = new FileReader();
    reader.onload = function(e) {
        const preview = document.createElement('div');
        preview.className = 'media-preview-item';
        preview.setAttribute('data-filename', file.name);

        let content = '';
        if (file.type.startsWith('image/')) {
            content = `<img src="${e.target.result}" alt="preview"><div class="media-preview-remove" data-filename="${escapeAttr(file.name)}">&times;</div>`;
        } else if (file.type.startsWith('video/')) {
            content = `<video src="${e.target.result}"></video><div class="media-preview-remove" data-filename="${escapeAttr(file.name)}">&times;</div><span class="media-preview-badge">Видео</span>`;
        } else if (file.type.startsWith('audio/')) {
            content = `<i class="bi bi-music-note-beamed"></i><div class="media-preview-remove" data-filename="${escapeAttr(file.name)}">&times;</div><span class="media-preview-badge">Аудио</span><span class="media-preview-name">${escapeHtml(file.name.substring(0, 20))}</span>`;
        } else {
            content = `${getFileIcon('DOCUMENT', file.name)}<div class="media-preview-remove" data-filename="${escapeAttr(file.name)}">&times;</div><span class="media-preview-badge">Файл</span><span class="media-preview-name">${escapeHtml(file.name.substring(0, 20))}</span>`;
        }

        preview.innerHTML = content;
        const removeBtn = preview.querySelector('.media-preview-remove');
        if (removeBtn) {
            removeBtn.addEventListener('click', () => removeMediaPreview(file.name));
        }
        document.getElementById('mediaPreviewList').appendChild(preview);
    };
    reader.readAsDataURL(file);
}

function removeMediaPreview(filename) {
    pendingFiles = pendingFiles.filter(f => f.name !== filename);
    const previews = document.querySelectorAll('.media-preview-item');
    previews.forEach(p => {
        if (p.getAttribute('data-filename') === filename) p.remove();
    });
    if (pendingFiles.length === 0) {
        document.getElementById('mediaPreviewContainer').style.display = 'none';
    }
}

async function sendMessageWithMedia() {
    const input = document.getElementById('messageInput');
    const content = input.value.trim();

    if (!content && pendingFiles.length === 0) return;

    if (!stompClient || !stompClient.connected) {
        showNotification('Нет соединения с сервером', 'error');
        return;
    }

    let mediaList = [];

    if (pendingFiles.length > 0) {
        isUploading = true;
        showNotification('Загрузка файлов...', 'info');

        const uploaded = await uploadFiles(pendingFiles);
        if (uploaded) {
            mediaList = uploaded.map((m, idx) => ({
                tempId: m.tempId,
                filename: m.filename,
                originalName: m.originalName,
                mediaType: m.mediaType,
                orderNum: idx,
                fileSize: m.fileSize,
                duration: m.duration,
                width: m.width,
                height: m.height
            }));
        } else {
            isUploading = false;
            showNotification('Не удалось загрузить файлы', 'error');
            return;
        }
        isUploading = false;
    }

    stompClient.send('/app/chat.send', {}, JSON.stringify({
        chatId: chatId,
        content: content,
        replyToMessageId: replyToMessageId,
        mediaList: mediaList
    }));

    input.value = '';
    cancelReply();
    pendingFiles = [];
    document.getElementById('mediaPreviewContainer').style.display = 'none';
    document.getElementById('mediaPreviewList').innerHTML = '';
}

function sendMessage() {
    sendMessageWithMedia();
}

// ============ Голосовые сообщения ============

async function startVoiceRecording() {
    if (mediaRecorder && mediaRecorder.state === 'recording') return;

    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
        showNotification('Запись голосовых не поддерживается в этом браузере', 'error');
        return;
    }

    try {
        voiceStream = await navigator.mediaDevices.getUserMedia({ audio: true });
    } catch (e) {
        showNotification('Нет доступа к микрофону', 'error');
        return;
    }

    voiceChunks = [];
    voiceCancelled = false;

    let mimeType = 'audio/webm;codecs=opus';
    if (!('MediaRecorder' in window) || !MediaRecorder.isTypeSupported(mimeType)) {
        if (window.MediaRecorder && MediaRecorder.isTypeSupported('audio/webm')) mimeType = 'audio/webm';
        else if (window.MediaRecorder && MediaRecorder.isTypeSupported('audio/ogg;codecs=opus')) mimeType = 'audio/ogg;codecs=opus';
        else mimeType = '';
    }

    try {
        mediaRecorder = mimeType ? new MediaRecorder(voiceStream, { mimeType }) : new MediaRecorder(voiceStream);
    } catch (e) {
        showNotification('Не удалось запустить запись', 'error');
        stopVoiceStream();
        return;
    }

    mediaRecorder.ondataavailable = e => {
        if (e.data && e.data.size > 0) voiceChunks.push(e.data);
    };

    mediaRecorder.onstop = async () => {
        stopVoiceStream();
        const durationSec = Math.round((Date.now() - voiceStartTime) / 1000);
        hideVoiceRecorder();

        if (voiceCancelled || voiceChunks.length === 0) return;
        if (durationSec < 1) {
            showNotification('Слишком короткая запись', 'error');
            return;
        }

        const blob = new Blob(voiceChunks, { type: mediaRecorder.mimeType || 'audio/webm' });
        showNotification('Отправка голосового...', 'info');
        const uploaded = await uploadVoice(blob, durationSec);
        if (!uploaded) {
            showNotification('Не удалось отправить голосовое', 'error');
            return;
        }

        const mediaList = [{
            tempId: uploaded.tempId,
            filename: uploaded.filename,
            originalName: uploaded.originalName,
            mediaType: 'VOICE_MESSAGE',
            orderNum: 0,
            fileSize: uploaded.fileSize,
            duration: durationSec
        }];

        if (stompClient && stompClient.connected) {
            stompClient.send('/app/chat.send', {}, JSON.stringify({
                chatId: chatId,
                content: '',
                replyToMessageId: replyToMessageId,
                mediaList: mediaList
            }));
            cancelReply();
        }
    };

    voiceStartTime = Date.now();
    mediaRecorder.start();
    showVoiceRecorder();
    voiceTimerId = setInterval(updateVoiceTimer, 200);
}

function stopVoiceStream() {
    if (voiceStream) {
        voiceStream.getTracks().forEach(t => t.stop());
        voiceStream = null;
    }
}

function updateVoiceTimer() {
    const elapsed = Math.floor((Date.now() - voiceStartTime) / 1000);
    const el = document.getElementById('voiceRecorderTime');
    if (el) el.textContent = formatDuration(elapsed);
    if (elapsed >= 300) finishVoiceRecording();
}

function showVoiceRecorder() {
    document.getElementById('voiceRecorderBar').style.display = 'flex';
    document.getElementById('messageInput').style.display = 'none';
    document.getElementById('voiceRecordBtn').classList.add('recording');

    const sendBtn = document.getElementById('sendBtn');
    if (sendBtn) sendBtn.style.display = 'none';
}

function hideVoiceRecorder() {
    document.getElementById('voiceRecorderBar').style.display = 'none';
    document.getElementById('messageInput').style.display = '';
    document.getElementById('voiceRecordBtn').classList.remove('recording');

    if (voiceTimerId) { clearInterval(voiceTimerId); voiceTimerId = null; }
    const el = document.getElementById('voiceRecorderTime');
    if (el) el.textContent = '00:00';

    const sendBtn = document.getElementById('sendBtn');
    if (sendBtn) sendBtn.style.display = '';
}

function finishVoiceRecording() {
    if (mediaRecorder && mediaRecorder.state === 'recording') {
        voiceCancelled = false;
        mediaRecorder.stop();
    }
}

function cancelVoiceRecording() {
    if (mediaRecorder && mediaRecorder.state === 'recording') {
        voiceCancelled = true;
        mediaRecorder.stop();
    } else {
        hideVoiceRecorder();
    }
}

// ============ end voice ============

function deleteMessage(messageId) {
    if (!confirm('Удалить сообщение?')) return;
    if (stompClient && stompClient.connected) {
        stompClient.send('/app/chat.delete', {}, JSON.stringify({ chatId: chatId, messageId: messageId }));
    }
}

function editMessage(messageId, oldContent) {
    const newContent = prompt('Редактировать сообщение:', oldContent);
    if (newContent && newContent.trim() && newContent !== oldContent) {
        if (stompClient && stompClient.connected) {
            stompClient.send('/app/chat.edit', {}, JSON.stringify({
                chatId: chatId,
                messageId: messageId,
                content: newContent
            }));
        }
    }
}

function replyToMessage(messageId, authorName, content) {
    replyToMessageId = messageId;
    const replyBar = document.getElementById('replyBar');
    replyBar.querySelector('span').innerHTML = `<i class="bi bi-reply-fill me-1"></i> Ответ ${escapeHtml(authorName)}: "${escapeHtml(content.substring(0, 50))}${content.length > 50 ? '...' : ''}"`;
    replyBar.style.display = 'flex';
    document.getElementById('messageInput').focus();
}

function cancelReply() {
    replyToMessageId = null;
    const replyBar = document.getElementById('replyBar');
    replyBar.style.display = 'none';
}

function loadMessages(page) {
    if (isLoading) return;
    isLoading = true;

    if (page === 0) {
        document.getElementById('messagesArea').innerHTML = '';
    }

    fetch('/api/chats/' + chatId + '/messages?page=' + page, { credentials: 'same-origin' })
        .then(function (r) { return r.json(); })
        .then(function (response) {
            const area = document.getElementById('messagesArea');
            const oldHeight = area.scrollHeight;
            const oldTop = area.scrollTop;

            if (response.content && response.content.length > 0) {
                response.content.forEach(function (msg) {
                    const isOut = msg.author?.username === currentUsername;
                    prependMessage(msg, isOut);
                });
                hasMore = !response.last;
            }

            if (page === 0) {
                scrollToBottom();
                const lastIncoming = getLastIncomingMessageId();
                if (lastIncoming) sendReadReceipt(lastIncoming);
            } else {
                area.scrollTop = oldTop + (area.scrollHeight - oldHeight);
            }
            isLoading = false;
        })
        .catch(function () { isLoading = false; });
}

function getLastIncomingMessageId() {
    const msgs = document.querySelectorAll('.message.incoming');
    if (msgs.length === 0) return null;
    return msgs[msgs.length - 1].getAttribute('data-id');
}

function appendMessage(msg, isOutgoing) {
    const area = document.getElementById('messagesArea');
    area.insertAdjacentHTML('beforeend', buildMessageHtml(msg, isOutgoing));
    scrollToBottom();
}

function prependMessage(msg, isOutgoing) {
    const area = document.getElementById('messagesArea');
    area.insertAdjacentHTML('afterbegin', buildMessageHtml(msg, isOutgoing));
}

function buildMediaHtml(mediaList) {
    if (!mediaList || mediaList.length === 0) return '';

    return '<div class="message-media">' + mediaList.map((media, idx) => {
        if (media.mediaType === 'IMAGE') {
            return `<div class="media-item media-image" onclick="openMediaViewer(${idx})">
                        <img src="${media.fileUrl}" alt="image" loading="lazy">
                    </div>`;
        } else if (media.mediaType === 'VIDEO') {
            return `<div class="media-item media-video" onclick="openMediaViewer(${idx})">
                        <video src="${media.fileUrl}" preload="metadata" muted></video>
                        <div class="video-play-btn"><i class="bi bi-play-fill"></i></div>
                    </div>`;
        } else if (media.mediaType === 'AUDIO') {
            return `<div class="media-item media-audio">
                        <i class="bi bi-music-note-beamed"></i>
                        <div class="audio-info">
                            <div class="audio-name">${escapeHtml(media.originalName || 'Аудио')}</div>
                            <audio controls preload="none" src="${media.fileUrl}"></audio>
                        </div>
                    </div>`;
        } else if (media.mediaType === 'VOICE_MESSAGE') {
            const dur = media.duration || 0;
            return `<div class="media-item media-voice" data-duration="${dur}">
                        <button class="voice-play-btn" onclick="toggleVoiceMessage(this, '${media.fileUrl}')">
                            <i class="bi bi-play-fill"></i>
                        </button>
                        <div class="voice-wave">
                            <div class="voice-wave-progress"></div>
                        </div>
                        <div class="voice-duration">${formatDuration(dur)}</div>
                        <audio style="display: none;" preload="metadata" src="${media.fileUrl}"></audio>
                    </div>`;
        } else {
            // DOCUMENT / OTHER — иконка + имя + размер + скачать
            return `<div class="media-item media-file">
                        ${getFileIcon(media.mediaType, media.originalName)}
                        <div class="file-info">
                            <div class="file-name">${escapeHtml(media.originalName || 'Файл')}</div>
                            <div class="file-size">${formatFileSize(media.fileSize)}</div>
                        </div>
                        <a href="${media.fileUrl}" download="${escapeAttr(media.originalName || 'download')}" target="_blank" rel="noopener" class="file-download" title="Скачать">
                            <i class="bi bi-download"></i>
                        </a>
                    </div>`;
        }
    }).join('') + '</div>';
}

function openMediaViewer(startIndex) {
    const messageDiv = event?.target?.closest('.message');
    if (!messageDiv) return;

    const mediaItems = messageDiv.querySelectorAll('.media-image, .media-video');
    currentMediaList = Array.from(mediaItems).map(item => {
        if (item.classList.contains('media-image')) {
            const img = item.querySelector('img');
            return { type: 'image', src: img?.src, element: item };
        } else {
            const video = item.querySelector('video');
            return { type: 'video', src: video?.src || video?.querySelector('source')?.src, element: item };
        }
    });
    currentMediaIndex = startIndex;
    updateMediaViewer();
    const modal = new bootstrap.Modal(document.getElementById('mediaViewerModal'));
    modal.show();
}

function updateMediaViewer() {
    const media = currentMediaList[currentMediaIndex];
    const container = document.getElementById('mediaViewerContent');
    const nav = document.getElementById('mediaViewerNav');
    const counter = document.getElementById('mediaCounter');

    if (currentMediaList.length > 1) {
        nav.style.display = 'flex';
        counter.textContent = `${currentMediaIndex + 1} / ${currentMediaList.length}`;
    } else {
        nav.style.display = 'none';
    }

    if (media.type === 'image') {
        container.innerHTML = `<img src="${media.src}" alt="media" style="max-width: 100%; max-height: 70vh;">`;
    } else {
        container.innerHTML = `<video src="${media.src}" controls autoplay style="max-width: 100%; max-height: 70vh;"></video>`;
    }
}

function nextMedia() {
    if (currentMediaIndex < currentMediaList.length - 1) {
        currentMediaIndex++;
        updateMediaViewer();
    }
}

function prevMedia() {
    if (currentMediaIndex > 0) {
        currentMediaIndex--;
        updateMediaViewer();
    }
}

function toggleVoiceMessage(btn, url) {
    const container = btn.closest('.media-voice');
    const audio = container.querySelector('audio');
    const icon = btn.querySelector('i');
    const progress = container.querySelector('.voice-wave-progress');
    const durationEl = container.querySelector('.voice-duration');
    const storedDuration = parseFloat(container.getAttribute('data-duration')) || 0;

    if (audio.paused) {
        document.querySelectorAll('.media-voice audio').forEach(a => {
            if (a !== audio) a.pause();
        });
        document.querySelectorAll('.voice-play-btn i').forEach(i => {
            i.className = 'bi bi-play-fill';
        });

        if (!audio.src) audio.src = url;

        audio.ontimeupdate = () => {
            const total = isFinite(audio.duration) && audio.duration > 0 ? audio.duration : storedDuration;
            if (total > 0 && progress) {
                progress.style.width = ((audio.currentTime / total) * 100) + '%';
            }
            if (durationEl) durationEl.textContent = formatDuration(audio.currentTime);
        };
        audio.onended = () => {
            icon.className = 'bi bi-play-fill';
            if (progress) progress.style.width = '0%';
            if (durationEl) durationEl.textContent = formatDuration(storedDuration);
        };
        audio.play().then(() => {
            icon.className = 'bi bi-pause-fill';
        }).catch(() => {
            icon.className = 'bi bi-play-fill';
        });
    } else {
        audio.pause();
        icon.className = 'bi bi-play-fill';
    }
}

function buildMessageHtml(msg, isOutgoing) {
    const time = msg.createdAt
        ? new Date(msg.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
        : '';
    const isDeleted = msg.status === 'DELETED';
    const isEdited = msg.isEdited && !isDeleted;
    const content = escapeHtml(msg.content || '');
    const authorName = escapeHtml(msg.author?.username || 'Пользователь');

    let statusHtml = '';
    if (isOutgoing && !isDeleted) {
        const tick = (msg.status === 'READ') ? '✓✓' : '✓';
        statusHtml = '<span class="message-status">' + tick + '</span>';
    }

    const editedHtml = isEdited ? '<span class="message-edited">(ред.)</span>' : '';

    let replyHtml = '';
    if (msg.replyToMessage && !isDeleted) {
        const replyAuthorName = escapeHtml(msg.replyToMessage.author?.username || 'Пользователь');
        const replyContent = escapeHtml(msg.replyToMessage.content || '');
        replyHtml = '<div class="message-reply" onclick="scrollToMessage(\'' + msg.replyToMessage.id + '\')">' +
            '<div class="message-reply-author">↳ ' + replyAuthorName + '</div>' +
            '<div class="message-reply-content">' + replyContent + '</div>' +
            '</div>';
    }

    let actionsHtml = '';
    if (!isDeleted) {
        if (isOutgoing) {
            const safeContent = escapeHtml(msg.content || '').replace(/'/g, "\\'");
            actionsHtml = '<span class="message-actions">' +
                '<button class="message-action-btn" onclick="editMessage(\'' + msg.id + '\', \'' + safeContent + '\')" title="Редактировать"><i class="bi bi-pencil-fill"></i></button>' +
                '<button class="message-action-btn" onclick="deleteMessage(\'' + msg.id + '\')" title="Удалить"><i class="bi bi-trash-fill"></i></button>' +
                '</span>';
        } else {
            const safeContent = escapeHtml(msg.content || '').replace(/'/g, "\\'");
            actionsHtml = '<span class="message-actions">' +
                '<button class="message-action-btn" onclick="replyToMessage(\'' + msg.id + '\', \'' + authorName + '\', \'' + safeContent + '\')" title="Ответить"><i class="bi bi-reply-fill"></i></button>' +
                '</span>';
        }
    }

    const authorLink = '<a href="/user/' + authorName + '" class="message-author-link" target="_blank">@' + authorName + '</a>';
    const mediaHtml = isDeleted ? '' : buildMediaHtml(msg.mediaList);

    return '<div class="message ' + (isOutgoing ? 'outgoing' : 'incoming') + (isDeleted ? ' deleted' : '') + '" data-id="' + msg.id + '">' +
        (!isOutgoing ? '<div class="message-meta" style="margin-bottom: 2px;">' + authorLink + '</div>' : '') +
        '<div class="message-bubble' + (isDeleted ? ' deleted' : '') + '">' + replyHtml + (isDeleted ? 'Сообщение удалено' : content) + mediaHtml + '</div>' +
        '<div class="message-meta">' + time + ' ' + statusHtml + editedHtml + actionsHtml + '</div>' +
        '</div>';
}

function scrollToMessage(messageId) {
    const el = document.querySelector('.message[data-id="' + messageId + '"]');
    if (el) {
        el.scrollIntoView({ behavior: 'smooth', block: 'center' });
        el.style.backgroundColor = 'rgba(139, 92, 246, 0.2)';
        setTimeout(() => {
            el.style.backgroundColor = '';
        }, 2000);
    }
}

function scrollToBottom() {
    const area = document.getElementById('messagesArea');
    area.scrollTop = area.scrollHeight;
}

function showNotification(message, type) {
    const alertDiv = document.createElement('div');
    alertDiv.className = `alert alert-${type === 'error' ? 'danger' : 'info'} alert-dismissible fade show position-fixed`;
    alertDiv.style.cssText = 'top: 80px; right: 20px; z-index: 9999; min-width: 300px;';
    alertDiv.innerHTML = `
        <i class="bi bi-${type === 'error' ? 'exclamation-triangle-fill' : 'info-circle-fill'} me-2"></i>
        ${escapeHtml(message)}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;
    document.body.appendChild(alertDiv);
    setTimeout(() => alertDiv.remove(), 3000);
}

function loadChats(page) {
    fetch('/api/chats?page=0&size=100', { credentials: 'same-origin' })
        .then(r => r.json())
        .then(response => {
            if (response.content) allChats = response.content;
            renderChatList(allChats);
        });
}

function renderChatList(chats) {
    const list = document.getElementById('chatsList');
    const searchQuery = document.getElementById('chatSearchInput')?.value.toLowerCase() || '';

    let filteredChats = chats;
    if (searchQuery) {
        filteredChats = chats.filter(chat =>
            (chat.interlocutor?.name || chat.interlocutor?.username || '').toLowerCase().includes(searchQuery)
        );
    }

    if (filteredChats.length === 0 && chats.length > 0) {
        list.innerHTML = '<div class="no-items-msg">Чаты не найдены</div>';
        return;
    }

    list.innerHTML = filteredChats.map(chat => {
        const isActive = chat.id.toString() === chatId.toString();
        const letter = (chat.interlocutor?.name || chat.interlocutor?.username || '?').charAt(0).toUpperCase();
        const avatarHtml = chat.interlocutor?.avatarUrl
            ? '<img src="' + chat.interlocutor.avatarUrl + '" alt="avatar">'
            : letter;
        const unread = chat.unreadCount > 0 ? '<span class="unread-badge">' + chat.unreadCount + '</span>' : '';
        const lastMsg = escapeHtml(chat.lastMessage?.content || 'Нет сообщений');
        const time = chat.lastMessage?.createdAt
            ? new Date(chat.lastMessage.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
            : '';

        return '<a class="chat-item' + (isActive ? ' active' : '') + '" href="/chat/' + chat.id + '" data-chat-id="' + chat.id + '">' +
            '<div class="chat-avatar">' + avatarHtml + '</div>' +
            '<div class="chat-info">' +
            '<div class="chat-name">' + escapeHtml(chat.interlocutor?.name || chat.interlocutor?.username || 'Пользователь') + '</div>' +
            '<div class="chat-last-msg">' + lastMsg + '</div>' +
            '</div>' +
            '<div class="chat-meta">' +
            '<span class="chat-time">' + time + '</span>' +
            unread +
            '</div>' +
            '</a>';
    }).join('');
}

function updateChatInList(chat) {
    const index = allChats.findIndex(c => c.id === chat.id);
    if (index !== -1) allChats[index] = chat;
    else allChats.unshift(chat);
    renderChatList(allChats);
}

function escapeHtml(text) {
    if (text === null || text === undefined) return '';
    return String(text)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

function escapeAttr(text) {
    return escapeHtml(text);
}

function sendTyping(isTyping) {
    if (!chatId) return;
    if (stompClient && stompClient.connected) {
        stompClient.send('/app/chat.typing', {}, JSON.stringify({ chatId: chatId, isTyping: isTyping }));
    }
}

function initAttachMenu() {
    const attachBtn = document.getElementById('attachBtn');
    const attachMenu = document.getElementById('attachMenu');

    if (attachBtn) {
        attachBtn.addEventListener('click', function(e) {
            e.stopPropagation();
            attachMenu.style.display = attachMenu.style.display === 'none' ? 'flex' : 'none';
        });

        document.addEventListener('click', function() {
            attachMenu.style.display = 'none';
        });

        attachMenu.addEventListener('click', function(e) {
            e.stopPropagation();
        });
    }

    const fileInput = document.createElement('input');
    fileInput.type = 'file';
    fileInput.multiple = true;
    fileInput.accept = '*/*';

    fileInput.onchange = async function(e) {
        const files = Array.from(e.target.files);
        if (pendingFiles.length + files.length > 10) {
            showNotification('Максимум 10 файлов на одно сообщение', 'error');
            return;
        }

        for (const file of files) {
            if (file.size > 50 * 1024 * 1024) {
                showNotification(`Файл ${file.name} превышает 50MB`, 'error');
                continue;
            }
            pendingFiles.push(file);
            addMediaToPreview(file);
        }

        if (pendingFiles.length > 0) {
            document.getElementById('mediaPreviewContainer').style.display = 'block';
        }
        fileInput.value = '';
    };

    document.getElementById('attachPhotoBtn')?.addEventListener('click', () => {
        fileInput.accept = 'image/*';
        fileInput.click();
        attachMenu.style.display = 'none';
    });

    document.getElementById('attachVideoBtn')?.addEventListener('click', () => {
        fileInput.accept = 'video/*';
        fileInput.click();
        attachMenu.style.display = 'none';
    });

    document.getElementById('attachAudioBtn')?.addEventListener('click', () => {
        fileInput.accept = 'audio/*';
        fileInput.click();
        attachMenu.style.display = 'none';
    });

    document.getElementById('attachDocumentBtn')?.addEventListener('click', () => {
        fileInput.accept = '.doc,.docx,.xls,.xlsx,.ppt,.pptx,.pdf,.txt,.md,.rtf,.odt,.ods,.odp';
        fileInput.click();
        attachMenu.style.display = 'none';
    });

    document.getElementById('attachOtherBtn')?.addEventListener('click', () => {
        fileInput.accept = '*/*';
        fileInput.click();
        attachMenu.style.display = 'none';
    });
}

async function loadFriends() {
    try {
        const response = await fetch('/api/friendships/api/friends', { credentials: 'same-origin' });
        if (response.ok) {
            let friends = await response.json();
            if (!Array.isArray(friends)) {
                friends = Object.values(friends);
            }
            renderFriendsList(friends);
        } else {
            document.getElementById('friendsListModal').innerHTML = '<div class="text-center py-4 text-secondary-custom">Ошибка загрузки списка друзей</div>';
        }
    } catch (error) {
        document.getElementById('friendsListModal').innerHTML = '<div class="text-center py-4 text-secondary-custom">Ошибка загрузки списка друзей</div>';
    }
}

function renderFriendsList(friends) {
    const container = document.getElementById('friendsListModal');
    if (!friends || friends.length === 0) {
        container.innerHTML = '<div class="text-center py-4 text-secondary-custom">У вас пока нет друзей</div>';
        return;
    }
    container.innerHTML = friends.map(friend => `
        <div class="friend-item" data-user-id="${friend.id}" data-username="${friend.username}" data-name="${friend.name || ''}">
            <div class="d-flex align-items-center flex-grow-1">
                <div class="friend-avatar">
                    ${friend.avatarUrl ? `<img src="${friend.avatarUrl}" alt="Avatar">` : `<span>${(friend.name || friend.username || 'U').charAt(0).toUpperCase()}</span>`}
                </div>
                <div class="friend-info">
                    <div class="friend-name">${escapeHtml(friend.name || friend.username)}</div>
                    <div class="friend-username">@${escapeHtml(friend.username)}</div>
                </div>
            </div>
            <button class="btn-start-chat" onclick="createChatWithUser('${friend.id}', '${friend.username}')">
                <i class="bi bi-chat-fill"></i> Написать
            </button>
        </div>
    `).join('');

    const searchInput = document.getElementById('friendSearchInput');
    if (searchInput) {
        const handler = function() {
            const query = this.value.toLowerCase().trim();
            const items = container.querySelectorAll('.friend-item');
            let hasVisible = false;
            items.forEach(item => {
                const username = item.getAttribute('data-username')?.toLowerCase() || '';
                const name = item.getAttribute('data-name')?.toLowerCase() || '';
                const isVisible = username.includes(query) || name.includes(query);
                item.style.display = isVisible ? 'flex' : 'none';
                if (isVisible) hasVisible = true;
            });
            if (!hasVisible && query) {
                if (!container.querySelector('.no-result-msg')) {
                    container.insertAdjacentHTML('beforeend', '<div class="no-result-msg text-center py-3 text-secondary-custom small">Ничего не найдено</div>');
                }
            } else {
                container.querySelector('.no-result-msg')?.remove();
            }
        };
        searchInput.addEventListener('input', handler);
    }
}

async function createChatWithUser(userId, username) {
    try {
        const response = await fetch('/api/chats', {
            method: 'POST',
            credentials: 'same-origin',
            headers: { 'Content-Type': 'application/json', [getCsrfHeader()]: getCsrfToken() },
            body: JSON.stringify({ secondUserId: userId })
        });
        if (response.ok) {
            const chat = await response.json();
            window.location.href = '/chat/' + chat.id;
        } else if (response.status === 400) {
            const chats = await fetch('/api/chats?page=0&size=100', { credentials: 'same-origin' }).then(r => r.json());
            const existing = (chats.content || []).find(c => c.interlocutor?.id === userId);
            if (existing) window.location.href = '/chat/' + existing.id;
        }
    } catch (error) {
        console.error('Error creating chat:', error);
    }
}

function initNewChatModal() {
    const modal = document.getElementById('newChatModal');
    if (modal) {
        modal.addEventListener('show.bs.modal', function() { loadFriends(); });
    }
}

function loadChatInfo() {
    fetch('/api/chats?page=0&size=100', { credentials: 'same-origin' })
        .then(r => r.json())
        .then(response => {
            const chat = (response.content || []).find(c => c.id.toString() === chatId.toString());
            if (chat && chat.interlocutor) {
                const username = chat.interlocutor.username || 'Пользователь';
                const displayName = chat.interlocutor.name || username;
                document.getElementById('chatTopbarName').innerHTML = '<a href="/user/' + encodeURIComponent(username) + '" class="chat-topbar-name-link" target="_blank" rel="noopener noreferrer">' + escapeHtml(displayName) + '</a>';
                const avatarEl = document.getElementById('chatTopbarAvatar');
                if (chat.interlocutor.avatarUrl) {
                    avatarEl.innerHTML = '<img src="' + chat.interlocutor.avatarUrl + '" alt="avatar">';
                } else {
                    avatarEl.innerHTML = (displayName.charAt(0) || '?').toUpperCase();
                }
            }
        });
}

document.addEventListener('DOMContentLoaded', function () {
    const input = document.getElementById('messageInput');
    const searchInput = document.getElementById('chatSearchInput');
    if (searchInput) {
        searchInput.addEventListener('input', function() { renderChatList(allChats); });
    }
    input.addEventListener('input', function () {
        clearTimeout(typingTimeout);
        sendTyping(true);
        typingTimeout = setTimeout(function () { sendTyping(false); }, 1500);
    });
    input.addEventListener('keydown', function (e) {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            sendMessage();
        }
    });
    document.getElementById('messagesArea').addEventListener('scroll', function () {
        if (this.scrollTop < 60 && hasMore && !isLoading) {
            currentPage++;
            loadMessages(currentPage);
        }
    });
    document.getElementById('sendBtn').addEventListener('click', sendMessage);
    document.getElementById('cancelReplyBtn').addEventListener('click', cancelReply);
    document.getElementById('mediaPrevBtn')?.addEventListener('click', prevMedia);
    document.getElementById('mediaNextBtn')?.addEventListener('click', nextMedia);

    document.getElementById('voiceRecordBtn')?.addEventListener('click', startVoiceRecording);
    document.getElementById('voiceStopBtn')?.addEventListener('click', finishVoiceRecording);
    document.getElementById('voiceCancelBtn')?.addEventListener('click', cancelVoiceRecording);

    initAttachMenu();
    initNewChatModal();
    loadChats();
    loadChatInfo();
    loadMessages(0);
    connectWebSocket();
});

document.addEventListener('DOMContentLoaded', function() {
    const toggleBtn = document.getElementById('chatToggleBtn');
    const sidebar = document.getElementById('chatSidebar');
    if (toggleBtn && sidebar) {
        toggleBtn.addEventListener('click', function() { sidebar.classList.toggle('open'); });
        document.addEventListener('click', function(e) {
            if (window.innerWidth <= 768 && sidebar.classList.contains('open') && !sidebar.contains(e.target) && !toggleBtn.contains(e.target)) {
                sidebar.classList.remove('open');
            }
        });
    }
});