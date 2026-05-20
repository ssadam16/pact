let userArticlesCurrentPage = 0;
let userArticlesTotalPages = 0;
let userArticlesLoading = false;

function getCsrfToken() {
    return document.querySelector('meta[name="_csrf"]')?.content;
}

function getCsrfHeader() {
    return document.querySelector('meta[name="_csrf_header"]')?.content;
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function formatDate(dateString) {
    const date = new Date(dateString);
    return date.toLocaleDateString('ru-RU', {
        day: 'numeric',
        month: 'short',
        year: 'numeric'
    });
}

function renderUserArticleCard(article) {
    return `
        <div class="user-article-card mb-3 p-3" style="background: var(--bg-elevated); border-radius: 16px; border: 1px solid var(--border-subtle);">
            <div class="d-flex flex-wrap justify-content-between align-items-start gap-2">
                <div class="flex-grow-1">
                    <a href="/article/${article.id}" class="fw-semibold text-decoration-none hover-lift" style="font-size: 1rem; color: var(--text-primary);">
                        ${escapeHtml(article.title)}
                    </a>
                    <div class="d-flex flex-wrap gap-3 mt-2">
                        <span class="small text-secondary-custom">
                            <i class="bi bi-calendar3"></i> ${formatDate(article.createdAt)}
                        </span>
                        <span class="small text-secondary-custom">
                            <i class="bi bi-heart${article.likesCount > 0 ? '-fill' : ''}" style="color: ${article.likesCount > 0 ? '#ef4444' : ''}"></i> ${article.likesCount}
                        </span>
                        <span class="small text-secondary-custom">
                            <i class="bi bi-chat-dots-fill"></i> ${article.commentsCount}
                        </span>
                    </div>
                    <div class="d-flex flex-wrap gap-1 mt-2">
                        ${article.tags?.slice(0, 3).map(tag => `<span class="tag" style="font-size: 0.65rem;">${escapeHtml(tag.name)}</span>`).join('')}
                    </div>
                </div>
                <a href="/article/${article.id}" class="btn-outline-pact-sm" style="white-space: nowrap;">
                    Читать <i class="bi bi-arrow-right"></i>
                </a>
            </div>
        </div>
    `;
}

async function loadStats() {
    const userId = window.profileId;
    if (!userId) return;

    try {
        const response = await fetch(`/api/stats?userId=${userId}`, {
            method: 'GET',
            headers: { 'Content-Type': 'application/json' }
        });

        if (response.ok) {
            const stats = await response.json();

            const articlesCountEl = document.getElementById('articlesCount');
            const commentsCountEl = document.getElementById('commentsCount');
            const likesCountEl = document.getElementById('likesCount');
            const daysCountEl = document.getElementById('daysCount');

            if (articlesCountEl) articlesCountEl.textContent = stats.articlesCount || 0;
            if (commentsCountEl) commentsCountEl.textContent = stats.commentsCount || 0;
            if (likesCountEl) likesCountEl.textContent = stats.likesCount || 0;
            if (daysCountEl) daysCountEl.textContent = stats.daysInCommunity || 0;
        }
    } catch (error) {
        console.error('Error loading stats:', error);
    }
}

async function loadUserArticles(page) {
    if (userArticlesLoading) return;
    userArticlesLoading = true;

    const container = document.getElementById('userArticlesContainer');
    const spinner = document.getElementById('userArticlesSpinner');
    const paginationContainer = document.getElementById('userArticlesPagination');

    spinner.style.display = 'block';
    container.innerHTML = '';
    paginationContainer.innerHTML = '';

    const authorId = window.profileId;
    if (!authorId) {
        spinner.style.display = 'none';
        userArticlesLoading = false;
        return;
    }

    const params = new URLSearchParams({
        page: page,
        size: 6,
        sortBy: 'createdAt',
        sortType: 'desc',
        authorId: authorId
    });

    try {
        const response = await fetch(`/articles?${params.toString()}`, {
            method: 'GET',
            headers: { 'Content-Type': 'application/json' }
        });

        if (response.ok) {
            const articles = await response.json();

            if (articles.length === 0 && page === 0) {
                container.innerHTML = '<div class="text-center py-4 text-secondary-custom">У автора пока нет статей</div>';
                userArticlesTotalPages = 0;
            } else {
                container.innerHTML = articles.map(article => renderUserArticleCard(article)).join('');
                userArticlesTotalPages = page + 1;
                if (articles.length < 6) userArticlesTotalPages = page + 1;
                else userArticlesTotalPages = page + 2;
            }
            renderUserArticlesPagination();
        }
    } catch (error) {
        console.error('Error loading user articles:', error);
        container.innerHTML = '<div class="text-center py-4 text-secondary-custom">Ошибка загрузки статей</div>';
    } finally {
        spinner.style.display = 'none';
        userArticlesLoading = false;
    }
}

function renderUserArticlesPagination() {
    const container = document.getElementById('userArticlesPagination');
    if (userArticlesTotalPages <= 1 && userArticlesCurrentPage === 0) {
        container.innerHTML = '';
        return;
    }

    let html = `
        <div class="d-flex justify-content-center gap-2">
            <button class="pagination-btn" id="userArticlesPrevBtn" ${userArticlesCurrentPage === 0 ? 'disabled' : ''}>
                <i class="bi bi-chevron-left"></i> Предыдущая
            </button>
            <span class="px-2 py-1 text-secondary-custom">${userArticlesCurrentPage + 1} / ${userArticlesTotalPages}</span>
            <button class="pagination-btn" id="userArticlesNextBtn" ${userArticlesCurrentPage >= userArticlesTotalPages - 1 ? 'disabled' : ''}>
                Следующая <i class="bi bi-chevron-right"></i>
            </button>
        </div>
    `;

    container.innerHTML = html;

    document.getElementById('userArticlesPrevBtn')?.addEventListener('click', () => {
        if (userArticlesCurrentPage > 0) {
            userArticlesCurrentPage--;
            loadUserArticles(userArticlesCurrentPage);
        }
    });

    document.getElementById('userArticlesNextBtn')?.addEventListener('click', () => {
        if (userArticlesCurrentPage < userArticlesTotalPages - 1) {
            userArticlesCurrentPage++;
            loadUserArticles(userArticlesCurrentPage);
        }
    });
}

document.addEventListener('DOMContentLoaded', function() {
    const avatarInput = document.getElementById('avatarInput');
    const avatarPreview = document.getElementById('avatarPreview');
    const avatarPreviewPlaceholder = document.getElementById('avatarPreviewPlaceholder');
    const avatarForm = document.getElementById('avatarForm');

    if (avatarInput) {
        avatarInput.addEventListener('change', function(e) {
            const file = e.target.files[0];
            if (file) {
                const reader = new FileReader();
                reader.onload = function(event) {
                    if (avatarPreview) {
                        avatarPreview.src = event.target.result;
                        avatarPreview.style.display = 'block';
                        if (avatarPreviewPlaceholder) {
                            avatarPreviewPlaceholder.style.display = 'none';
                        }
                    } else if (avatarPreviewPlaceholder) {
                        const img = document.createElement('img');
                        img.id = 'avatarPreview';
                        img.className = 'hexagon-preview-img';
                        img.src = event.target.result;
                        img.alt = 'Avatar preview';
                        const parent = avatarPreviewPlaceholder.parentNode;
                        parent.insertBefore(img, avatarPreviewPlaceholder);
                        avatarPreviewPlaceholder.style.display = 'none';
                    }
                };
                reader.readAsDataURL(file);
            }
        });
    }

    if (avatarForm) {
        avatarForm.addEventListener('submit', function(e) {
            const fileInput = document.getElementById('avatarInput');
            if (fileInput && !fileInput.files.length) {
                e.preventDefault();
                const errorDiv = document.createElement('div');
                errorDiv.className = 'alert alert-danger mt-3';
                errorDiv.innerHTML = '<i class="bi bi-exclamation-triangle-fill me-2"></i> Пожалуйста, выберите файл для загрузки';

                const existingError = avatarForm.querySelector('.alert-danger');
                if (existingError) {
                    existingError.remove();
                }

                avatarForm.querySelector('.modal-body').appendChild(errorDiv);

                setTimeout(() => {
                    errorDiv.remove();
                }, 3000);
            }
        });
    }

    if (window.profileId) {
        loadStats();
        loadUserArticles(0);
    }
});