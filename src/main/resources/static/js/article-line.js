let currentPage = 0;
let totalPages = 0;
let isLoading = false;
let currentView = 'grid';
let currentFilters = {
    search: '',
    tags: [],
    games: [],
    sortBy: 'createdAt',
    sortType: 'desc'
};

let allGames = [];
let allTags = [];

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

function renderArticleCard(article) {
    const gamesHtml = article.games?.slice(0, 2).map(game =>
        `<span class="tag" style="font-size: 0.65rem;">${escapeHtml(game.name)}</span>`
    ).join('') + (article.games?.length > 2 ? `<span class="tag" style="font-size: 0.65rem;">+${article.games.length - 2}</span>` : '');

    const tagsHtml = article.tags?.slice(0, 3).map(tag =>
        `<span class="tag" style="font-size: 0.65rem;">${escapeHtml(tag.name)}</span>`
    ).join('') + (article.tags?.length > 3 ? `<span class="tag" style="font-size: 0.65rem;">+${article.tags.length - 3}</span>` : '');

    return `
        <div class="article-card" data-article-id="${article.id}">
            <div class="card-body">
                <div class="card-meta">
                    <a href="/user/${article.author.username}" class="text-decoration-none hover-lift" style="color: var(--accent-purple);">
                        <i class="bi bi-person-circle"></i> ${escapeHtml(article.author.username)}
                    </a>
                    <span><i class="bi bi-calendar3"></i> ${formatDate(article.createdAt)}</span>
                </div>
                <a href="/article/${article.id}" class="card-title text-decoration-none hover-lift">
                    ${escapeHtml(article.title)}
                </a>
                <div class="card-tags">
                    ${gamesHtml}
                    ${tagsHtml}
                </div>
                <div class="card-stats">
                    <span><i class="bi bi-heart"></i> ${article.likesCount}</span>
                    <span><i class="bi bi-chat-dots-fill"></i> ${article.commentsCount}</span>
                </div>
            </div>
        </div>
    `;
}

function renderArticleRow(article) {
    const gamesHtml = article.games?.slice(0, 2).map(game =>
        `<span class="tag" style="font-size: 0.65rem;">${escapeHtml(game.name)}</span>`
    ).join('') + (article.games?.length > 2 ? `<span class="tag" style="font-size: 0.65rem;">+${article.games.length - 2}</span>` : '');

    const tagsHtml = article.tags?.slice(0, 2).map(tag =>
        `<span class="tag" style="font-size: 0.65rem;">${escapeHtml(tag.name)}</span>`
    ).join('') + (article.tags?.length > 2 ? `<span class="tag" style="font-size: 0.65rem;">+${article.tags.length - 2}</span>` : '');

    return `
        <div class="article-card" data-article-id="${article.id}">
            <div class="card-body" style="display: flex; flex-wrap: wrap; align-items: center; gap: 1rem;">
                <div style="flex: 2; min-width: 150px;">
                    <a href="/article/${article.id}" class="fw-semibold text-decoration-none hover-lift" style="color: var(--text-primary);">
                        ${escapeHtml(article.title)}
                    </a>
                </div>
                <div style="flex: 1; min-width: 100px;">
                    <a href="/user/${article.author.username}" class="text-decoration-none hover-lift" style="color: var(--accent-purple);">
                        ${escapeHtml(article.author.username)}
                    </a>
                </div>
                <div style="flex: 1.5; min-width: 120px;">
                    ${gamesHtml}
                    ${tagsHtml}
                </div>
                <div style="flex: 0.5; min-width: 80px;">
                    <span><i class="bi bi-heart"></i> ${article.likesCount}</span>
                    <span class="ms-2"><i class="bi bi-chat-dots-fill"></i> ${article.commentsCount}</span>
                </div>
                <div style="flex: 0.5; min-width: 80px;">
                    ${formatDate(article.createdAt)}
                </div>
            </div>
        </div>
    `;
}

async function loadArticles() {
    if (isLoading) return;
    isLoading = true;

    const container = document.getElementById('articlesContainer');
    const spinner = document.getElementById('loadingSpinner');
    spinner.style.display = 'block';
    container.innerHTML = '';

    const params = new URLSearchParams({
        page: currentPage,
        size: 30,
        sortBy: currentFilters.sortBy,
        sortType: currentFilters.sortType
    });

    if (currentFilters.search) params.append('search', currentFilters.search);
    if (currentFilters.tags.length > 0) currentFilters.tags.forEach(t => params.append('tags', t));
    if (currentFilters.games.length > 0) currentFilters.games.forEach(g => params.append('games', g));

    try {
        const response = await fetch(`/articles?${params.toString()}`, {
            method: 'GET',
            headers: { 'Content-Type': 'application/json' }
        });

        if (response.ok) {
            let articles = await response.json();

            if (currentFilters.sortBy === 'logScore') {
                articles = articles.sort((a, b) => {
                    const scoreA = Math.log((a.likesCount || 0) + (a.commentsCount || 0) + 1) * 100;
                    const scoreB = Math.log((b.likesCount || 0) + (b.commentsCount || 0) + 1) * 100;
                    return currentFilters.sortType === 'desc' ? scoreB - scoreA : scoreA - scoreB;
                });
            }

            if (articles.length === 0 && currentPage === 0) {
                container.innerHTML = '<div class="no-articles"><i class="bi bi-journal-x"></i><p>Статьи не найдены</p></div>';
                totalPages = 0;
            } else {
                renderArticles(articles);
                totalPages = currentPage + 1;
                if (articles.length < 30) totalPages = currentPage + 1;
                else totalPages = currentPage + 2;
            }
            renderPagination();
        }
    } catch (error) {
        console.error('Error loading articles:', error);
        container.innerHTML = '<div class="no-articles"><i class="bi bi-exclamation-triangle-fill"></i><p>Ошибка загрузки статей</p></div>';
    } finally {
        isLoading = false;
        spinner.style.display = 'none';
    }
}

function renderArticles(articles) {
    const container = document.getElementById('articlesContainer');
    container.className = `articles-container ${currentView}-view`;

    if (currentView === 'grid') {
        container.innerHTML = articles.map(article => renderArticleCard(article)).join('');
    } else {
        container.innerHTML = `
            <div class="article-card" style="background: transparent; margin-bottom: 0.5rem; border: none;">
                <div class="card-body" style="display: flex; flex-wrap: wrap; gap: 1rem; font-weight: 600; color: var(--accent-purple);">
                    <div style="flex: 2; min-width: 150px;">Название</div>
                    <div style="flex: 1; min-width: 100px;">Автор</div>
                    <div style="flex: 1.5; min-width: 120px;">Теги/Игры</div>
                    <div style="flex: 0.5; min-width: 80px;">Статистика</div>
                    <div style="flex: 0.5; min-width: 80px;">Дата</div>
                </div>
            </div>
            ${articles.map(article => renderArticleRow(article)).join('')}
        `;
    }
}

function renderPagination() {
    const container = document.getElementById('paginationContainer');
    if (totalPages <= 1 && currentPage === 0) {
        container.innerHTML = '';
        return;
    }

    let pages = [];
    const maxVisible = 5;
    let startPage = Math.max(0, currentPage - Math.floor(maxVisible / 2));
    let endPage = Math.min(totalPages - 1, startPage + maxVisible - 1);
    if (endPage - startPage + 1 < maxVisible) startPage = Math.max(0, endPage - maxVisible + 1);

    for (let i = startPage; i <= endPage; i++) pages.push(i);

    let html = `
        <button class="pagination-btn" id="prevPageBtn" ${currentPage === 0 ? 'disabled' : ''}>‹ Назад</button>
    `;

    if (startPage > 0) html += `<button class="pagination-btn" data-page="0">1</button><span class="px-1 text-secondary-custom">...</span>`;

    pages.forEach(page => {
        html += `<button class="pagination-btn ${page === currentPage ? 'active' : ''}" data-page="${page}">${page + 1}</button>`;
    });

    if (endPage < totalPages - 1) html += `<span class="px-1 text-secondary-custom">...</span><button class="pagination-btn" data-page="${totalPages - 1}">${totalPages}</button>`;

    html += `<button class="pagination-btn" id="nextPageBtn" ${currentPage >= totalPages - 1 ? 'disabled' : ''}>Вперед ›</button>`;

    container.innerHTML = html;

    document.getElementById('prevPageBtn')?.addEventListener('click', () => goToPage(currentPage - 1));
    document.getElementById('nextPageBtn')?.addEventListener('click', () => goToPage(currentPage + 1));
    document.querySelectorAll('.pagination-btn[data-page]').forEach(btn => {
        btn.addEventListener('click', () => goToPage(parseInt(btn.getAttribute('data-page'))));
    });
}

function goToPage(page) {
    if (page === currentPage || page < 0 || page >= totalPages) return;
    currentPage = page;
    loadArticles();
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

function updateFilters() {
    currentFilters.search = document.getElementById('searchInput')?.value || '';
    currentFilters.tags = Array.from(document.querySelectorAll('.tag-checkbox-input:checked')).map(cb => cb.value);

    const gamesSelect = document.getElementById('gamesFilter');
    currentFilters.games = Array.from(gamesSelect?.selectedOptions || []).map(opt => opt.value);

    const sortValue = document.getElementById('sortSelect')?.value || 'createdAt,desc';
    const [sortBy, sortType] = sortValue.split(',');
    currentFilters.sortBy = sortBy;
    currentFilters.sortType = sortType;

    currentPage = 0;
    loadArticles();
}

function initFiltersPanel() {
    const toggleBtn = document.getElementById('filterToggleBtn');
    const panel = document.getElementById('filtersPanel');
    toggleBtn?.addEventListener('click', () => {
        const isVisible = panel.style.display === 'block';
        panel.style.display = isVisible ? 'none' : 'block';
    });

    document.getElementById('applyFiltersBtn')?.addEventListener('click', updateFilters);
    document.getElementById('resetFiltersBtn')?.addEventListener('click', () => {
        document.getElementById('searchInput').value = '';
        document.querySelectorAll('.tag-checkbox-input').forEach(cb => cb.checked = false);
        const gamesSelect = document.getElementById('gamesFilter');
        if (gamesSelect) Array.from(gamesSelect.options).forEach(opt => opt.selected = false);
        document.getElementById('sortSelect').value = 'createdAt,desc';
        updateFilters();
    });

    document.getElementById('searchInput')?.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') updateFilters();
    });

    document.getElementById('sortSelect')?.addEventListener('change', updateFilters);
}

function initViewToggle() {
    const btns = document.querySelectorAll('.view-toggle-btn');
    btns.forEach(btn => {
        btn.addEventListener('click', () => {
            btns.forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            currentView = btn.getAttribute('data-view');
            loadArticles();
        });
    });
}

document.addEventListener('DOMContentLoaded', function() {
    allGames = window.gamesData || [];
    allTags = window.tagsData || [];

    initFiltersPanel();
    initViewToggle();
    loadArticles();
});