/* new-chat.js */

let foundUser = null;

function searchUser() {
    const username = document.getElementById('usernameInput').value.trim();

    if (!username) {
        showError('Введите username пользователя');
        return;
    }

    showError('');
    document.getElementById('userResult').classList.remove('show');
    document.getElementById('searchBtn').disabled = true;
    document.getElementById('searchBtn').textContent = 'Поиск...';

    fetch('/api/users/' + encodeURIComponent(username), { credentials: 'same-origin' })
        .then(function (r) {
            if (!r.ok) throw r;
            return r.json();
        })
        .then(function (user) {
            foundUser = user;
            renderUserResult(user);
            document.getElementById('userResult').classList.add('show');
        })
        .catch(function (r) {
            if (r.status === 404) {
                showError('Пользователь не найден');
            } else {
                showError('Ошибка при поиске пользователя');
            }
            foundUser = null;
        })
        .finally(function () {
            document.getElementById('searchBtn').disabled = false;
            document.getElementById('searchBtn').textContent = 'Найти';
        });
}

function renderUserResult(user) {
    const letter = (user.name || user.username || '?').charAt(0).toUpperCase();
    const avatarHtml = user.avatarUrl
        ? '<img src="' + user.avatarUrl + '" alt="avatar">'
        : letter;

    document.getElementById('userResultInfo').innerHTML =
        '<div class="user-result-info">' +
        '<div class="chat-avatar">' + avatarHtml + '</div>' +
        '<div>' +
        '<div class="user-result-name">' + escapeHtml(user.name || user.username) + '</div>' +
        '<div class="user-result-username">@' + escapeHtml(user.username) + '</div>' +
        '</div>' +
        '</div>';
}

function createChat() {
    if (!foundUser) return;

    document.getElementById('startChatBtn').disabled = true;
    document.getElementById('startChatBtn').textContent = 'Создание...';

    fetch('/api/chats', {
        method: 'POST',
        credentials: 'same-origin',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ secondUserId: foundUser.id })
    })
        .then(function (r) {
            if (!r.ok) throw r;
            return r.json();
        })
        .then(function (chat) {
            window.location.href = '/chat/' + chat.id;
        })
        .catch(function (r) {
            if (r.status === 400) {
                // Chat might already exist — find it
                findExistingChat();
            } else {
                showError('Ошибка создания чата');
                document.getElementById('startChatBtn').disabled = false;
                document.getElementById('startChatBtn').textContent = 'Начать чат';
            }
        });
}

function findExistingChat() {
    fetch('/api/chats?page=0&size=100', { credentials: 'same-origin' })
        .then(function (r) { return r.json(); })
        .then(function (response) {
            const existing = (response.content || []).find(function (c) {
                return c.interlocutor?.id === foundUser.id;
            });
            if (existing) {
                window.location.href = '/chat/' + existing.id;
            } else {
                showError('Не удалось создать чат');
                document.getElementById('startChatBtn').disabled = false;
                document.getElementById('startChatBtn').textContent = 'Начать чат';
            }
        })
        .catch(function () {
            showError('Не удалось создать чат');
            document.getElementById('startChatBtn').disabled = false;
            document.getElementById('startChatBtn').textContent = 'Начать чат';
        });
}

function showError(message) {
    const el = document.getElementById('errorMessage');
    el.textContent = message;
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
    document.getElementById('usernameInput').addEventListener('keydown', function (e) {
        if (e.key === 'Enter') searchUser();
    });
    document.getElementById('searchBtn').addEventListener('click', searchUser);
    document.getElementById('startChatBtn').addEventListener('click', createChat);
});
