/* room.js */

const chatId = window.__CHAT_ID__;
const currentUsername = window.__CURRENT_USERNAME__;

let stompClient = null;
let currentPage = 0;
let isLoading = false;
let hasMore = true;
let typingTimeout = null;

/* ── WebSocket ── */

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
                // New message
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
                indicator.textContent = 'Печатает...';
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

/* ── Send Message ── */

function sendMessage() {
    const input = document.getElementById('messageInput');
    const content = input.value.trim();
    if (!content) return;

    if (!stompClient || !stompClient.connected) {
        alert('Нет соединения с сервером');
        return;
    }

    stompClient.send('/app/chat.send', {}, JSON.stringify({
        chatId: chatId,
        content: content,
        replyToMessageId: null,
        mediaList: null
    }));

    input.value = '';
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

/* ── Load Messages ── */

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
                // Mark first-page incoming messages as read via REST path
                // (WebSocket read receipt also sent below for each incoming msg)
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

/* ── Render Messages ── */

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

    let statusHtml = '';
    if (isOutgoing) {
        const tick = (msg.status === 'READ') ? '✓✓' : '✓';
        statusHtml = '<span class="message-status">' + tick + '</span>';
    }

    const editedHtml = isEdited ? '<span class="message-edited">(ред.)</span>' : '';

    let actionsHtml = '';
    if (isOutgoing && !isDeleted) {
        const safeContent = escapeHtml(msg.content || '').replace(/'/g, "\\'");
        actionsHtml = '<span class="message-actions">' +
            '<button class="message-action-btn" onclick="editMessage(\'' + msg.id + '\', \'' + safeContent + '\')">✎</button>' +
            '<button class="message-action-btn" onclick="deleteMessage(\'' + msg.id + '\')">✕</button>' +
            '</span>';
    }

    return '<div class="message ' + (isOutgoing ? 'outgoing' : 'incoming') + '" data-id="' + msg.id + '">' +
        '<div class="message-bubble' + (isDeleted ? ' deleted' : '') + '">' + content + '</div>' +
        '<div class="message-meta">' + time + ' ' + statusHtml + editedHtml + actionsHtml + '</div>' +
        '</div>';
}

function scrollToBottom() {
    const area = document.getElementById('messagesArea');
    area.scrollTop = area.scrollHeight;
}

/* ── Chats Sidebar ── */

function loadChats() {
    fetch('/api/chats?page=0&size=100', { credentials: 'same-origin' })
        .then(function (r) { return r.json(); })
        .then(function (response) {
            const list = document.getElementById('chatsList');
            list.innerHTML = '';
            if (response.content && response.content.length > 0) {
                response.content.forEach(function (chat) {
                    list.insertAdjacentHTML('beforeend', buildChatItemHtml(chat));
                });
            } else {
                list.innerHTML = '<div class="no-items-msg">Нет чатов</div>';
            }
        });
}

function loadChatInfo() {
    fetch('/api/chats?page=0&size=100', { credentials: 'same-origin' })
        .then(function (r) { return r.json(); })
        .then(function (response) {
            const chat = (response.content || []).find(function (c) {
                return c.id.toString() === chatId.toString();
            });
            if (chat && chat.interlocutor) {
                document.getElementById('chatTopbarName').textContent =
                    chat.interlocutor.name || chat.interlocutor.username || 'Пользователь';
                const avatarEl = document.getElementById('chatTopbarAvatar');
                if (chat.interlocutor.avatarUrl) {
                    avatarEl.innerHTML = '<img src="' + chat.interlocutor.avatarUrl + '" alt="avatar">';
                } else {
                    const letter = (chat.interlocutor.name || chat.interlocutor.username || '?').charAt(0).toUpperCase();
                    avatarEl.textContent = letter;
                }
            }
        });
}

function buildChatItemHtml(chat) {
    const isActive = chat.id.toString() === chatId.toString();
    const letter = (chat.interlocutor?.name || chat.interlocutor?.username || '?').charAt(0).toUpperCase();
    const avatarHtml = chat.interlocutor?.avatarUrl
        ? '<img src="' + chat.interlocutor.avatarUrl + '" alt="avatar">'
        : letter;

    const unread = chat.unreadCount > 0
        ? '<span class="unread-badge">' + chat.unreadCount + '</span>'
        : '';

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
}

function updateChatInList(chat) {
    const existing = document.querySelector('[data-chat-id="' + chat.id + '"]');
    const html = buildChatItemHtml(chat);
    if (existing) {
        existing.outerHTML = html;
    } else {
        document.getElementById('chatsList').insertAdjacentHTML('afterbegin', html);
    }
}

/* ── Typing ── */

function sendTyping(isTyping) {
    if (!chatId) return;
    if (stompClient && stompClient.connected) {
        stompClient.send('/app/chat.typing', {}, JSON.stringify({ chatId: chatId, isTyping: isTyping }));
    }
}

/* ── Utils ── */

function escapeHtml(text) {
    if (!text) return '';
    return String(text)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}

/* ── Init ── */

document.addEventListener('DOMContentLoaded', function () {
    const input = document.getElementById('messageInput');

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

    loadChats();
    loadChatInfo();
    loadMessages(0);
    connectWebSocket();
});