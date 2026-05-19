function getCsrfToken() {
    return document.querySelector('meta[name="_csrf"]')?.content;
}

function getCsrfHeader() {
    return document.querySelector('meta[name="_csrf_header"]')?.content;
}

function showNotification(message, type = 'info') {
    const alertDiv = document.createElement('div');
    alertDiv.className = `alert alert-${type === 'error' ? 'danger' : type === 'success' ? 'success' : 'info'} alert-dismissible fade show position-fixed`;
    alertDiv.style.cssText = 'top: 80px; right: 20px; z-index: 9999; min-width: 300px;';
    alertDiv.innerHTML = `
        <i class="bi bi-${type === 'error' ? 'exclamation-triangle-fill' : type === 'success' ? 'check-circle-fill' : 'info-circle-fill'} me-2"></i>
        ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;

    document.body.appendChild(alertDiv);

    setTimeout(() => {
        alertDiv.remove();
    }, 3000);
}

async function searchUserByUsername(username) {
    if (!username || username.trim() === '') {
        document.getElementById('searchResults').style.display = 'none';
        return;
    }

    try {
        const response = await fetch(`/api/friendships/search/${encodeURIComponent(username)}`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                [getCsrfHeader()]: getCsrfToken()
            }
        });

        if (response.ok) {
            const user = await response.json();
            displaySearchResult(user);
        } else if (response.status === 204) {
            displayMessage('Пользователь уже в друзьях или запрос отправлен');
        } else if (response.status === 404) {
            displayNoResults();
        } else {
            displayNoResults();
        }
    } catch (error) {
        console.error('Search error:', error);
        displayNoResults();
    }
}

function displayMessage(message) {
    const resultsContainer = document.getElementById('searchResultsList');
    const resultsWrapper = document.getElementById('searchResults');
    resultsContainer.innerHTML = `<div class="text-center py-3 text-secondary-custom small">${message}</div>`;
    resultsWrapper.style.display = 'block';
}

function displaySearchResult(user) {
    const resultsContainer = document.getElementById('searchResultsList');
    const resultsWrapper = document.getElementById('searchResults');

    resultsContainer.innerHTML = `
        <div class="search-result-item">
            <div class="d-flex align-items-center gap-3">
                <div class="friend-avatar-sm">
                    ${user.avatarUrl
        ? `<img src="${user.avatarUrl}" alt="Avatar" class="rounded-circle" width="48" height="48" style="object-fit: cover;">`
        : `<div class="friend-avatar-placeholder-sm">${user.username.charAt(0).toUpperCase()}</div>`
    }
                </div>
                <div>
                    <a href="/user/${user.username}" class="fw-semibold text-decoration-none hover-lift">
                        ${user.username}
                    </a>
                </div>
            </div>
            <button class="btn-add-friend" data-user-id="${user.id}">
                <i class="bi bi-person-plus-fill"></i> Добавить
            </button>
        </div>
    `;

    const addButton = resultsContainer.querySelector('.btn-add-friend');
    addButton.addEventListener('click', function() {
        const userId = this.getAttribute('data-user-id');
        sendFriendRequest(userId, this);
    });

    resultsWrapper.style.display = 'block';
}

function displayNoResults() {
    const resultsContainer = document.getElementById('searchResultsList');
    const resultsWrapper = document.getElementById('searchResults');

    resultsContainer.innerHTML = '<div class="text-center py-3 text-secondary-custom small">Пользователь не найден</div>';
    resultsWrapper.style.display = 'block';
}

async function sendFriendRequest(userId, button) {
    try {
        const response = await fetch(`/api/friendships/request/${userId}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                [getCsrfHeader()]: getCsrfToken()
            }
        });

        if (response.ok) {
            button.disabled = true;
            button.className = 'btn-pending';
            button.innerHTML = '<i class="bi bi-clock-history"></i> Запрос отправлен';
            showNotification('Запрос в друзья отправлен', 'success');
        } else {
            const error = await response.text();
            showNotification(error || 'Ошибка при отправке запроса', 'error');
        }
    } catch (error) {
        console.error('Send request error:', error);
        showNotification('Ошибка соединения с сервером', 'error');
    }
}

async function acceptRequest(requestId, button) {
    try {
        const response = await fetch(`/api/friendships/${requestId}/accept`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                [getCsrfHeader()]: getCsrfToken()
            }
        });

        if (response.ok) {
            const requestItem = button.closest('.request-item');
            requestItem.remove();
            updateRequestsCount();
            showNotification('Заявка принята', 'success');
            setTimeout(() => {
                location.reload();
            }, 1000);
        } else {
            const error = await response.text();
            showNotification(error || 'Ошибка при принятии заявки', 'error');
        }
    } catch (error) {
        console.error('Accept request error:', error);
        showNotification('Ошибка соединения с сервером', 'error');
    }
}

async function declineRequest(requestId, button) {
    try {
        const response = await fetch(`/api/friendships/${requestId}/decline`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                [getCsrfHeader()]: getCsrfToken()
            }
        });

        if (response.ok) {
            const requestItem = button.closest('.request-item');
            requestItem.remove();
            updateRequestsCount();
            showNotification('Заявка отклонена', 'info');
        } else {
            const error = await response.text();
            showNotification(error || 'Ошибка при отклонении заявки', 'error');
        }
    } catch (error) {
        console.error('Decline request error:', error);
        showNotification('Ошибка соединения с сервером', 'error');
    }
}

async function removeFriend(friendId, button) {
    if (!confirm('Вы уверены, что хотите удалить этого пользователя из друзей?')) {
        return;
    }

    try {
        const response = await fetch(`/api/friendships/${friendId}/remove`, {
            method: 'DELETE',
            headers: {
                'Content-Type': 'application/json',
                [getCsrfHeader()]: getCsrfToken()
            }
        });

        if (response.ok) {
            const friendCard = button.closest('.col-sm-6');
            friendCard.remove();
            updateFriendsCount();
            showNotification('Друг удален', 'info');
        } else {
            const error = await response.text();
            showNotification(error || 'Ошибка при удалении друга', 'error');
        }
    } catch (error) {
        console.error('Remove friend error:', error);
        showNotification('Ошибка соединения с сервером', 'error');
    }
}

function updateRequestsCount() {
    const requestsList = document.getElementById('requestsList');
    const requests = requestsList.querySelectorAll('.request-item');
    const countSpan = document.getElementById('requestsCount');
    const newCount = requests.length;

    countSpan.textContent = newCount;

    if (newCount === 0) {
        requestsList.innerHTML = `
            <div class="text-center py-4">
                <i class="bi bi-inbox fs-1 text-secondary-custom"></i>
                <p class="text-secondary-custom mt-2 mb-0">Нет входящих заявок</p>
            </div>
        `;
    }
}

function updateFriendsCount() {
    const friendsList = document.getElementById('friendsList');
    const friends = friendsList.querySelectorAll('.col-sm-6');
    const countSpan = document.getElementById('friendsCount');
    countSpan.textContent = friends.length;

    if (friends.length === 0) {
        friendsList.innerHTML = `
            <div class="text-center py-5">
                <i class="bi bi-people fs-1 text-secondary-custom"></i>
                <p class="text-secondary-custom mt-2 mb-0">У вас пока нет друзей</p>
                <p class="text-secondary-custom small">Найдите интересных людей через поиск</p>
            </div>
        `;
    }
}

document.addEventListener('DOMContentLoaded', function() {
    const searchInput = document.getElementById('searchInput');
    const searchBtn = document.getElementById('searchBtn');

    let debounceTimer;
    searchInput.addEventListener('input', function() {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(() => {
            searchUserByUsername(searchInput.value);
        }, 500);
    });

    searchBtn.addEventListener('click', function() {
        searchUserByUsername(searchInput.value);
    });

    document.addEventListener('click', function(e) {
        const resultsWrapper = document.getElementById('searchResults');
        if (resultsWrapper && !resultsWrapper.contains(e.target) && e.target !== searchInput && e.target !== searchBtn) {
            resultsWrapper.style.display = 'none';
        }
    });

    const acceptButtons = document.querySelectorAll('.btn-accept');
    acceptButtons.forEach(button => {
        button.addEventListener('click', function() {
            const requestId = this.getAttribute('data-request-id');
            if (requestId) {
                acceptRequest(requestId, this);
            }
        });
    });

    const declineButtons = document.querySelectorAll('.btn-decline');
    declineButtons.forEach(button => {
        button.addEventListener('click', function() {
            const requestId = this.getAttribute('data-request-id');
            if (requestId) {
                declineRequest(requestId, this);
            }
        });
    });

    const removeButtons = document.querySelectorAll('.btn-remove-friend');
    removeButtons.forEach(button => {
        button.addEventListener('click', function() {
            const friendId = this.getAttribute('data-friend-id');
            if (friendId) {
                removeFriend(friendId, this);
            }
        });
    });
});