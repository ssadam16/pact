const chatId = window.__CHAT_ID__;
const currentUsername = window.__CURRENT_USERNAME__;

let stompClient = null;
let currentPage = 0;
let isLoading = false;
let hasMore = true;
let typingTimeout = null;
let replyToMessageId = null;
let allChats = [];

function getCsrfToken() {
    return document.querySelector('meta[name="_csrf"]')?.content;
}

function getCsrfHeader() {
    return document.querySelector('meta[name="_csrf_header"]')?.content;
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
                    el.querySelector('.message-bubble').textContent = 'Сообщение удалено';
                    el.querySelector('.message-bubble').classList.add('deleted');
                }
            } else if (data.action === 'EDIT') {
                const el = document.querySelector('.message[data-id="' + data.message.id + '"]');
                if (el) {
                    const isOut = data.message.author?.username === currentUsername;
                    el.outerHTML = buildMessageHtml(data.message, isOut);
                }
            } else if (data.action === 'READ') {
                if (data.chatId === chatId || data.chatId.toString() === chatId.toString()) {
                    markAllOutgoingRead();
                }
            } else {
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

function sendMessage() {
    const input = document.getElementById('messageInput');
    const content = input.value.trim();
    if (!content) return;

    if (!stompClient || !stompClient.connected) {
        showNotification('Нет соединения с сервером', 'error');
        return;
    }

    stompClient.send('/app/chat.send', {}, JSON.stringify({
        chatId: chatId,
        content: content,
        replyToMessageId: replyToMessageId,
        mediaList: null
    }));

    input.value = '';
    cancelReply();
}

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
    const replyText = replyBar.querySelector('span');
    replyText.innerHTML = `<i class="bi bi-reply-fill me-1"></i> Ответ ${authorName}: "${content.substring(0, 50)}${content.length > 50 ? '...' : ''}"`;
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

function buildMessageHtml(msg, isOutgoing) {
    const time = msg.createdAt
        ? new Date(msg.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
        : '';
    const isDeleted = msg.status === 'DELETED';
    const isEdited = msg.isEdited && !isDeleted;
    const content = escapeHtml(msg.content || '');
    const authorName = escapeHtml(msg.author?.username || 'Пользователь');
    const authorId = msg.author?.id || '';

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

    // Кликабельный ник — ссылка на профиль
    const authorLink = '<a href="/user/' + authorName + '" class="message-author-link" target="_blank">@' + authorName + '</a>';

    return '<div class="message ' + (isOutgoing ? 'outgoing' : 'incoming') + (isDeleted ? ' deleted' : '') + '" data-id="' + msg.id + '">' +
        (!isOutgoing ? '<div class="message-meta" style="margin-bottom: 2px;">' + authorLink + '</div>' : '') +
        '<div class="message-bubble' + (isDeleted ? ' deleted' : '') + '">' + replyHtml + (isDeleted ? 'Сообщение удалено' : content) + '</div>' +
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
        ${message}
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
    if (!text) return '';
    return String(text)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}

function sendTyping(isTyping) {
    if (!chatId) return;
    if (stompClient && stompClient.connected) {
        stompClient.send('/app/chat.typing', {}, JSON.stringify({ chatId: chatId, isTyping: isTyping }));
    }
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
            console.error('Failed to load friends:', response.status);
            document.getElementById('friendsListModal').innerHTML =
                '<div class="text-center py-4 text-secondary-custom">Ошибка загрузки списка друзей</div>';
        }
    } catch (error) {
        console.error('Error loading friends:', error);
        document.getElementById('friendsListModal').innerHTML =
            '<div class="text-center py-4 text-secondary-custom">Ошибка загрузки списка друзей</div>';
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
                    ${friend.avatarUrl
        ? `<img src="${friend.avatarUrl}" alt="Avatar">`
        : `<span>${(friend.name || friend.username || 'U').charAt(0).toUpperCase()}</span>`
    }
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
        const existingListener = searchInput._listener;
        if (existingListener) {
            searchInput.removeEventListener('input', existingListener);
        }
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
                const noResult = container.querySelector('.no-result-msg');
                if (!noResult) {
                    container.insertAdjacentHTML('beforeend', '<div class="no-result-msg text-center py-3 text-secondary-custom small">Ничего не найдено</div>');
                }
            } else {
                const noResult = container.querySelector('.no-result-msg');
                if (noResult) noResult.remove();
            }
        };
        searchInput.addEventListener('input', handler);
        searchInput._listener = handler;
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
        modal.addEventListener('show.bs.modal', function() {
            loadFriends();
        });
    }
}

document.addEventListener('DOMContentLoaded', function () {
    const input = document.getElementById('messageInput');
    const searchInput = document.getElementById('chatSearchInput');

    if (searchInput) {
        searchInput.addEventListener('input', function() {
            renderChatList(allChats);
        });
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

    initNewChatModal();
    loadChats();
    loadChatInfo();
    loadMessages(0);
    connectWebSocket();
});

function loadChatInfo() {
    fetch('/api/chats?page=0&size=100', { credentials: 'same-origin' })
        .then(r => r.json())
        .then(response => {
            const chat = (response.content || []).find(c => c.id.toString() === chatId.toString());
            if (chat && chat.interlocutor) {
                const username = chat.interlocutor.username || 'Пользователь';
                const displayName = chat.interlocutor.name || username;

                const nameElement = document.getElementById('chatTopbarName');
                nameElement.innerHTML = '<a href="/user/' + encodeURIComponent(username) + '" class="chat-topbar-name-link" target="_blank" rel="noopener noreferrer">' + escapeHtml(displayName) + '</a>';

                const avatarEl = document.getElementById('chatTopbarAvatar');
                if (chat.interlocutor.avatarUrl) {
                    avatarEl.innerHTML = '<img src="' + chat.interlocutor.avatarUrl + '" alt="avatar">';
                } else {
                    avatarEl.innerHTML = (displayName.charAt(0) || '?').toUpperCase();
                }
            }
        });
}

document.addEventListener('DOMContentLoaded', function() {
    const toggleBtn = document.getElementById('chatToggleBtn');
    const sidebar = document.getElementById('chatSidebar');

    if (toggleBtn && sidebar) {
        toggleBtn.addEventListener('click', function() {
            sidebar.classList.toggle('open');
        });

        document.addEventListener('click', function(e) {
            if (window.innerWidth <= 768 &&
                sidebar.classList.contains('open') &&
                !sidebar.contains(e.target) &&
                !toggleBtn.contains(e.target)) {
                sidebar.classList.remove('open');
            }
        });
    }
});