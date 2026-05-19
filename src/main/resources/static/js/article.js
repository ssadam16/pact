let currentPage = 0;
let isLoading = false;
let hasMore = true;
let articleId = null;
let isAuthenticated = false;

function getCsrfToken() {
    return document.querySelector('meta[name="_csrf"]')?.content;
}

function getCsrfHeader() {
    return document.querySelector('meta[name="_csrf_header"]')?.content;
}

function showNotification(message, type = 'info') {
    const alertDiv = document.createElement('div');
    alertDiv.className = `alert alert-${type === 'error' ? 'danger' : 'success'} alert-dismissible fade show position-fixed`;
    alertDiv.style.cssText = 'top: 80px; right: 20px; z-index: 9999; min-width: 300px;';
    alertDiv.innerHTML = `
        <i class="bi bi-${type === 'error' ? 'exclamation-triangle-fill' : 'check-circle-fill'} me-2"></i>
        ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;
    document.body.appendChild(alertDiv);
    setTimeout(() => alertDiv.remove(), 5000);
}

function getAvatarHtml(avatarUrl, username) {
    if (avatarUrl) {
        return `<img src="${avatarUrl}" alt="Avatar">`;
    } else {
        return `<span>${username ? username.charAt(0).toUpperCase() : 'U'}</span>`;
    }
}

function renderComment(comment) {
    const date = new Date(comment.createdAt);
    const formattedDate = date.toLocaleDateString('ru-RU', {
        day: 'numeric',
        month: 'long',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });

    return `
        <div class="comment-item d-flex gap-3">
            <div class="flex-shrink-0">
                <div class="comment-avatar">
                    ${getAvatarHtml(comment.author?.avatarUrl, comment.author?.username)}
                </div>
            </div>
            <div class="flex-grow-1">
                <div class="d-flex flex-wrap align-items-center gap-2">
                    <a href="/user/${comment.author?.username}" class="comment-author text-decoration-none hover-lift">
                        ${escapeHtml(comment.author?.username || 'Пользователь')}
                    </a>
                    <span class="comment-date">${formattedDate}</span>
                </div>
                <div class="comment-text">
                    ${escapeHtml(comment.content)}
                </div>
            </div>
        </div>
    `;
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

async function loadComments(reset = false) {
    if (isLoading) return;
    if (reset) {
        currentPage = 0;
        hasMore = true;
        const commentsList = document.getElementById('commentsList');
        if (commentsList) commentsList.innerHTML = '';
    }
    if (!hasMore && !reset) return;

    isLoading = true;

    try {
        const response = await fetch(`/api/comments/byArticleId/${articleId}?page=${currentPage}&size=10`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json'
            }
        });

        if (response.ok) {
            const page = await response.json();
            const comments = page.content;
            const totalElements = page.totalElements;

            const commentsList = document.getElementById('commentsList');
            const noCommentsPlaceholder = document.getElementById('noCommentsPlaceholder');
            const commentsCountSpan = document.getElementById('commentsCount');
            const commentsCountHeader = document.getElementById('commentsCountHeader');

            if (commentsCountSpan) {
                commentsCountSpan.textContent = totalElements;
            }
            if (commentsCountHeader) {
                commentsCountHeader.textContent = totalElements;
            }

            if (reset && commentsList) {
                commentsList.innerHTML = '';
            }

            if (comments.length === 0 && currentPage === 0) {
                if (noCommentsPlaceholder) noCommentsPlaceholder.style.display = 'block';
                if (commentsList) {
                    commentsList.innerHTML = '';
                    commentsList.appendChild(noCommentsPlaceholder);
                }
            } else {
                if (noCommentsPlaceholder) noCommentsPlaceholder.style.display = 'none';

                comments.forEach(comment => {
                    const commentHtml = renderComment(comment);
                    const tempDiv = document.createElement('div');
                    tempDiv.innerHTML = commentHtml;
                    if (commentsList) commentsList.appendChild(tempDiv.firstElementChild);
                });
            }

            hasMore = !page.last;
            const loadMoreContainer = document.getElementById('loadMoreContainer');
            if (loadMoreContainer) {
                loadMoreContainer.style.display = hasMore ? 'block' : 'none';
            }
            if (hasMore) {
                currentPage++;
            }
        }
    } catch (error) {
        console.error('Error loading comments:', error);
    } finally {
        isLoading = false;
    }
}

async function submitComment() {
    if (!isAuthenticated) {
        showNotification('Войдите в систему, чтобы оставить комментарий', 'error');
        return;
    }

    const content = document.getElementById('commentInput').value.trim();

    if (!content) {
        showNotification('Введите текст комментария', 'error');
        return;
    }

    if (content.length < 1 || content.length > 500) {
        showNotification('Комментарий должен быть от 1 до 500 символов', 'error');
        return;
    }

    const commentData = {
        content: content,
        articleId: articleId
    };

    try {
        const response = await fetch('/api/comments', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                [getCsrfHeader()]: getCsrfToken()
            },
            body: JSON.stringify(commentData)
        });

        const result = await response.json();

        if (response.ok && result.success) {
            document.getElementById('commentInput').value = '';
            showNotification('Комментарий добавлен', 'success');
            currentPage = 0;
            hasMore = true;
            await loadComments(true);
        } else {
            const errors = result.errors;
            if (errors) {
                const firstError = Object.values(errors)[0];
                showNotification(firstError || 'Ошибка при добавлении комментария', 'error');
            } else {
                showNotification('Ошибка при добавлении комментария', 'error');
            }
        }
    } catch (error) {
        console.error('Error submitting comment:', error);
        showNotification('Ошибка соединения с сервером', 'error');
    }
}

async function toggleLike() {
    if (!isAuthenticated) {
        showNotification('Войдите в систему, чтобы поставить лайк', 'error');
        return;
    }

    const likeBtn = document.getElementById('likeBtn');
    const likesSpan = likeBtn.querySelector('span:not(.bi)');
    const icon = likeBtn.querySelector('i');
    const isLiked = likeBtn.getAttribute('data-liked') === 'true';
    const url = isLiked ? `/articles/${articleId}/unlike` : `/articles/${articleId}/like`;

    try {
        const response = await fetch(url, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                [getCsrfHeader()]: getCsrfToken()
            }
        });

        if (response.ok) {
            const article = await response.json();

            if (!isLiked) {
                likeBtn.setAttribute('data-liked', 'true');
                icon.classList.remove('bi-heart');
                icon.classList.add('bi-heart-fill');
                icon.style.color = '#ef4444';
                likesSpan.textContent = article.likesCount;
                showNotification('Лайк поставлен', 'success');
            } else {
                likeBtn.setAttribute('data-liked', 'false');
                icon.classList.remove('bi-heart-fill');
                icon.classList.add('bi-heart');
                icon.style.color = '';
                likesSpan.textContent = article.likesCount;
                showNotification('Лайк убран', 'info');
            }
        } else {
            showNotification('Ошибка при изменении лайка', 'error');
        }
    } catch (error) {
        console.error('Error toggling like:', error);
        showNotification('Ошибка соединения с сервером', 'error');
    }
}

document.addEventListener('DOMContentLoaded', async function() {
    articleId = window.articleId;
    isAuthenticated = window.currentUser === true;

    const images = document.querySelectorAll('.article-content img');
    images.forEach(img => {
        img.setAttribute('loading', 'lazy');
    });

    const links = document.querySelectorAll('.article-content a');
    links.forEach(link => {
        if (link.hostname !== window.location.hostname) {
            link.setAttribute('target', '_blank');
            link.setAttribute('rel', 'noopener noreferrer');
        }
    });

    if (articleId) {
        await loadComments();

        const submitBtn = document.getElementById('submitCommentBtn');
        if (submitBtn) {
            submitBtn.addEventListener('click', submitComment);
        }

        const loadMoreBtn = document.getElementById('loadMoreBtn');
        if (loadMoreBtn) {
            loadMoreBtn.addEventListener('click', () => loadComments());
        }

        const commentInput = document.getElementById('commentInput');
        if (commentInput) {
            commentInput.addEventListener('keypress', function(e) {
                if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) {
                    e.preventDefault();
                    submitComment();
                }
            });
        }

        const likeBtn = document.getElementById('likeBtn');
        if (likeBtn) {
            likeBtn.addEventListener('click', toggleLike);
        }

        if (isAuthenticated && window.currentUsername) {
            const currentUserAvatar = document.getElementById('currentUserAvatar');
            if (currentUserAvatar) {
                currentUserAvatar.style.display = 'flex';
                if (window.currentUserAvatar) {
                    currentUserAvatar.innerHTML = `<img src="${window.currentUserAvatar}" alt="Avatar" class="rounded-circle" width="40" height="40" style="object-fit: cover;">`;
                } else {
                    currentUserAvatar.innerHTML = `<span>${window.currentUsername.charAt(0).toUpperCase()}</span>`;
                }
            }
        }
    }
});