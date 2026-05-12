/* chats.js */

let currentPage = 0;
let isLoading = false;
let hasMore = true;
let stompClient = null;

function connectWebSocket() {
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    stompClient.debug = null;

    stompClient.connect({}, function () {
        stompClient.subscribe('/user/queue/chats_update', function (message) {
            const chat = JSON.parse(message.body);
            updateChatInList(chat);
        });
    });
}

function loadChats(page) {
    if (isLoading) return;
    isLoading = true;

    fetch('/api/chats?page=' + page + '&size=30', { credentials: 'same-origin' })
        .then(function (r) { return r.json(); })
        .then(function (response) {
            const list = document.getElementById('chatsList');

            if (page === 0) list.innerHTML = '';

            if (response.content && response.content.length > 0) {
                response.content.forEach(function (chat) {
                    list.insertAdjacentHTML('beforeend', buildChatItemHtml(chat));
                });
                hasMore = !response.last;
            } else if (page === 0) {
                list.innerHTML = '<div class="no-items-msg">Нет чатов.<br>Начните новый разговор!</div>';
            }
            isLoading = false;
        })
        .catch(function () {
            if (page === 0) {
                document.getElementById('chatsList').innerHTML = '<div class="no-items-msg">Ошибка загрузки</div>';
            }
            isLoading = false;
        });
}

function buildChatItemHtml(chat) {
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

    return '<a class="chat-item" href="/chat/' + chat.id + '" data-chat-id="' + chat.id + '">' +
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
    const list = document.getElementById('chatsList');

    if (existing) {
        existing.outerHTML = html;
    } else {
        list.insertAdjacentHTML('afterbegin', html);
    }

    // Move updated chat to top
    const updatedEl = document.querySelector('[data-chat-id="' + chat.id + '"]');
    if (updatedEl) list.prepend(updatedEl);
}

function escapeHtml(text) {
    if (!text) return '';
    return String(text)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}

document.addEventListener('DOMContentLoaded', function () {
    document.getElementById('chatsList').addEventListener('scroll', function () {
        if (this.scrollTop + this.clientHeight >= this.scrollHeight - 60 && hasMore && !isLoading) {
            currentPage++;
            loadChats(currentPage);
        }
    });

    loadChats(0);
    connectWebSocket();
});
