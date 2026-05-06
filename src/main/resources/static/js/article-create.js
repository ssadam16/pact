const uploadUrl = '/api/images/upload';

let selectedGames = [];
let selectedTags = [];
let allGames = [];
let allTags = [];

function getCsrfToken() {
    return document.querySelector('meta[name="_csrf"]')?.content;
}

function getCsrfHeader() {
    return document.querySelector('meta[name="_csrf_header"]')?.content;
}

const quill = new Quill('#editor', {
    theme: 'snow',
    modules: {
        toolbar: {
            container: [
                [{ 'header': [1, 2, 3, 4, false] }],
                ['bold', 'italic', 'underline', 'strike'],
                [{ 'list': 'ordered' }, { 'list': 'bullet' }],
                [{ 'indent': '-1' }, { 'indent': '+1' }],
                [{ 'align': [] }],
                ['blockquote', 'code-block'],
                ['link', 'image', 'video'],
                ['clean']
            ],
            handlers: {
                'image': imageHandler
            }
        }
    }
});

async function imageHandler() {
    const input = document.createElement('input');
    input.setAttribute('type', 'file');
    input.setAttribute('accept', 'image/jpeg,image/png,image/gif,image/webp');
    input.click();

    input.onchange = async () => {
        const file = input.files[0];
        if (!file) return;

        if (file.size > 10 * 1024 * 1024) {
            showNotification('Файл слишком большой. Максимум 10MB', 'error');
            return;
        }

        const allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'];
        if (!allowedTypes.includes(file.type)) {
            showNotification('Неподдерживаемый формат. Используйте JPG, PNG, GIF или WEBP', 'error');
            return;
        }

        const formData = new FormData();
        formData.append('image', file);
        const range = quill.getSelection(true);
        quill.insertEmbed(range.index, 'image', '/loading.svg', 'user');

        try {
            const response = await fetch(uploadUrl, {
                method: 'POST',
                body: formData,
                headers: { [getCsrfHeader()]: getCsrfToken() }
            });
            const data = await response.json();

            if (response.ok && data.url) {
                quill.deleteText(range.index, 1);
                quill.insertEmbed(range.index, 'image', data.url);
            } else {
                quill.deleteText(range.index, 1);
                showNotification(data.error || 'Ошибка загрузки изображения', 'error');
            }
        } catch (error) {
            quill.deleteText(range.index, 1);
            showNotification('Ошибка соединения с сервером', 'error');
        }
    };
}

function showNotification(message, type = 'info') {
    const alertDiv = document.createElement('div');
    alertDiv.className = `alert alert-${type === 'error' ? 'danger' : 'info'} alert-dismissible fade show position-fixed`;
    alertDiv.style.cssText = 'top: 80px; right: 20px; z-index: 9999; min-width: 300px;';
    alertDiv.innerHTML = `
        <i class="bi bi-${type === 'error' ? 'exclamation-triangle-fill' : 'info-circle-fill'} me-2"></i>
        ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;
    document.body.appendChild(alertDiv);
    setTimeout(() => alertDiv.remove(), 5000);
}

function renderSelectedGames() {
    const container = document.getElementById('selectedGamesContainer');
    const hiddenInput = document.getElementById('gameIds');

    container.innerHTML = selectedGames.map(game => `
        <div class="selected-item" data-game-id="${game.id}">
            ${game.name}
            <span class="remove-item" onclick="removeGame('${game.id}')">&times;</span>
        </div>
    `).join('');

    hiddenInput.value = selectedGames.map(g => g.id).join(',');
}

function renderSelectedTags() {
    const container = document.getElementById('selectedTagsContainer');
    const hiddenInput = document.getElementById('tagIds');

    container.innerHTML = selectedTags.map(tag => `
        <div class="selected-item" data-tag-id="${tag.id}">
            ${tag.name}
            <span class="remove-item" onclick="removeTag('${tag.id}')">&times;</span>
        </div>
    `).join('');

    hiddenInput.value = selectedTags.map(t => t.id).join(',');
}

function removeGame(gameId) {
    selectedGames = selectedGames.filter(g => g.id !== gameId);
    renderSelectedGames();
}

function removeTag(tagId) {
    selectedTags = selectedTags.filter(t => t.id !== tagId);
    renderSelectedTags();
}

function addGame(game) {
    if (selectedGames.length >= 3) {
        showNotification('Нельзя выбрать более 3 игр', 'error');
        return false;
    }
    if (selectedGames.some(g => g.id === game.id)) {
        showNotification('Эта игра уже добавлена', 'error');
        return false;
    }
    selectedGames.push(game);
    renderSelectedGames();
    return true;
}

function addTag(tag) {
    if (selectedTags.length >= 5) {
        showNotification('Нельзя выбрать более 5 тегов', 'error');
        return false;
    }
    if (selectedTags.some(t => t.id === tag.id)) {
        showNotification('Этот тег уже добавлен', 'error');
        return false;
    }
    selectedTags.push(tag);
    renderSelectedTags();
    return true;
}

function initGameSearch() {
    const searchInput = document.getElementById('gameSearchInput');
    const dropdown = document.getElementById('gameSearchDropdown');

    if (!searchInput) return;

    searchInput.addEventListener('input', function() {
        const query = this.value.toLowerCase();
        if (query.length < 2) {
            dropdown.style.display = 'none';
            return;
        }

        const filtered = allGames.filter(game =>
            game.name.toLowerCase().includes(query) &&
            !selectedGames.some(g => g.id === game.id)
        );

        if (filtered.length === 0) {
            dropdown.innerHTML = '<div class="game-search-item text-secondary-custom">Ничего не найдено</div>';
        } else {
            dropdown.innerHTML = filtered.map(game => `
                <div class="game-search-item" onclick="addGame({id: '${game.id}', name: '${game.name.replace(/'/g, "\\'")}'})">
                    ${game.name}
                </div>
            `).join('');
        }
        dropdown.style.display = 'block';
    });

    document.addEventListener('click', function(e) {
        if (!searchInput.contains(e.target) && !dropdown.contains(e.target)) {
            dropdown.style.display = 'none';
        }
    });
}

function initTagSelect() {
    const tagSelect = document.getElementById('tagSelect');
    if (!tagSelect) return;

    tagSelect.addEventListener('change', function() {
        const selectedId = this.value;
        if (!selectedId) return;

        const tag = allTags.find(t => t.id === selectedId);
        if (tag) {
            addTag(tag);
        }
        this.value = '';
    });
}

function submitArticle() {
    const content = quill.root.innerHTML;
    document.getElementById('content').value = content;

    const title = document.querySelector('[name="title"]')?.value;
    if (!title || title.trim().length < 10) {
        showNotification('Название статьи должно быть не менее 10 символов', 'error');
        return;
    }

    const emptyContent = content === '<p><br></p>' || content === '' || content === '<p></p>';
    if (emptyContent || content.length < 300) {
        showNotification('Содержание статьи должно быть не менее 300 символов', 'error');
        return;
    }

    document.getElementById('articleForm').submit();
}

document.addEventListener('DOMContentLoaded', function() {
    allGames = window.gamesData || [];
    allTags = window.tagsData || [];

    initGameSearch();
    initTagSelect();

    const existingGameIds = document.getElementById('gameIds')?.value;
    if (existingGameIds) {
        const ids = existingGameIds.split(',').filter(id => id);
        ids.forEach(id => {
            const game = allGames.find(g => g.id === id);
            if (game && !selectedGames.some(g => g.id === id)) {
                selectedGames.push(game);
            }
        });
        renderSelectedGames();
    }

    const existingTagIds = document.getElementById('tagIds')?.value;
    if (existingTagIds) {
        const ids = existingTagIds.split(',').filter(id => id);
        ids.forEach(id => {
            const tag = allTags.find(t => t.id === id);
            if (tag && !selectedTags.some(t => t.id === id)) {
                selectedTags.push(tag);
            }
        });
        renderSelectedTags();
    }
});