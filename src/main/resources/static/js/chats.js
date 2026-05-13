let currentPage = 0;
let isLoading = false;
let hasMore = true;
let stompClient = null;
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
                if (page === 0) allChats = [...response.content];
                else allChats.push(...response.content);

                renderChatList(allChats);
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

function renderChatList(chats) {
    const list = document.getElementById('chatsList');
    const searchQuery = document.getElementById('chatSearchInput')?.value.toLowerCase() || '';

    let filteredChats = chats;
    if (searchQuery) {
        filteredChats = chats.filter(chat =>
            (chat.interlocutor?.name || chat.interlocutor?.username || '').toLowerCase().includes(searchQuery)
        );
    }

    if (filteredChats.length === 0) {
        list.innerHTML = '<div class="no-items-msg">Чаты не найдены</div>';
        return;
    }

    list.innerHTML = filteredChats.map(chat => buildChatItemHtml(chat)).join('');
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
    const searchInput = document.getElementById('chatSearchInput');
    if (searchInput) {
        searchInput.addEventListener('input', function() {
            renderChatList(allChats);
        });
    }

    initNewChatModal();
    loadChats(0);
    connectWebSocket();
});

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