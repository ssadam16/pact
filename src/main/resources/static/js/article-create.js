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
                ['image', 'video'],
                ['clean']
            ],
            handlers: {
                'image': imageHandler,
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

        if (file.size > 50 * 1024 * 1024) {
            showNotification('Файл слишком большой. Максимум 50MB', 'error');
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

    if (!container) return;

    container.innerHTML = selectedGames.map(game => `
        <div class="selected-item" data-game-id="${game.id}">
            ${escapeHtml(game.name)}
            <span class="remove-item" onclick="removeGame('${game.id}')">&times;</span>
        </div>
    `).join('');

    const gameIdsValue = selectedGames.map(g => g.id).join(',');
    if (hiddenInput) hiddenInput.value = gameIdsValue;
}

function renderSelectedTags() {
    const container = document.getElementById('selectedTagsContainer');
    const hiddenInput = document.getElementById('tagIds');

    if (!container) return;

    container.innerHTML = selectedTags.map(tag => `
        <div class="selected-item" data-tag-id="${tag.id}">
            ${escapeHtml(tag.name)}
            <span class="remove-item" onclick="removeTag('${tag.id}')">&times;</span>
        </div>
    `).join('');

    const tagIdsValue = selectedTags.map(t => t.id).join(',');
    if (hiddenInput) hiddenInput.value = tagIdsValue;
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
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

    const searchInput = document.getElementById('gameSearchInput');
    const dropdown = document.getElementById('gameSearchDropdown');
    if (searchInput) searchInput.value = '';
    if (dropdown) dropdown.style.display = 'none';

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
                <div class="game-search-item" data-game-id="${game.id}" data-game-name="${escapeHtml(game.name).replace(/'/g, "\\'")}">
                    ${escapeHtml(game.name)}
                </div>
            `).join('');

            const items = dropdown.querySelectorAll('.game-search-item');
            items.forEach(item => {
                item.removeEventListener('click', handleGameClick);
                item.addEventListener('click', handleGameClick);
            });
        }
        dropdown.style.display = 'block';
    });

    function handleGameClick(e) {
        const item = e.currentTarget;
        const gameId = item.getAttribute('data-game-id');
        const gameName = item.getAttribute('data-game-name');
        const game = allGames.find(g => g.id === gameId);
        if (game) {
            addGame(game);
        }
    }

    document.addEventListener('click', function(e) {
        if (!searchInput.contains(e.target) && !dropdown.contains(e.target)) {
            dropdown.style.display = 'none';
        }
    });
}

function initCustomTagSelect() {
    const trigger = document.getElementById('tagTrigger');
    const dropdown = document.getElementById('tagDropdown');
    const searchInput = document.getElementById('tagSearchInput');
    const optionsContainer = document.getElementById('tagOptions');

    if (!trigger) return;

    trigger.addEventListener('click', function() {
        const isVisible = dropdown.style.display === 'block';
        dropdown.style.display = isVisible ? 'none' : 'block';
        if (!isVisible && searchInput) {
            searchInput.value = '';
            filterTagOptions('');
        }
    });

    if (searchInput) {
        searchInput.addEventListener('input', function() {
            filterTagOptions(this.value.toLowerCase());
        });
    }

    function filterTagOptions(query) {
        if (!optionsContainer) return;

        const originalOptions = optionsContainer.querySelectorAll('.custom-select-option');
        originalOptions.forEach(option => {
            const tagName = option.getAttribute('data-tag-name');
            if (tagName) {
                const isVisible = tagName.toLowerCase().includes(query);
                option.style.display = isVisible ? 'block' : 'none';
            }
        });
    }

    document.addEventListener('click', function(e) {
        if (!trigger.contains(e.target) && !dropdown.contains(e.target)) {
            dropdown.style.display = 'none';
        }
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

    const textContent = content.replace(/<[^>]*>/g, '').trim();
    if (textContent.length < 300) {
        showNotification('Содержание статьи должно быть не менее 300 символов', 'error');
        return;
    }

    document.getElementById('articleForm').submit();
}

async function improveText() {
    const editorContent = quill.root.innerText;

    if (!editorContent || editorContent.trim().length === 0) {
        showNotification('Нет текста для улучшения', 'error');
        return;
    }

    if (editorContent.trim().length < 50) {
        showNotification('Текст слишком короткий (минимум 300 символов)', 'error');
        return;
    }

    showNotification('ИИ обрабатывает текст...', 'info');

    try {
        const response = await fetch('/api/ai/improve', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                [getCsrfHeader()]: getCsrfToken()
            },
            body: JSON.stringify({ text: editorContent })
        });

        const data = await response.json();

        if (response.ok && data.improvedText) {
            const range = quill.getSelection();
            if (range) {
                quill.deleteText(0, quill.getLength());
                quill.setText(data.improvedText);
            } else {
                quill.setText(data.improvedText);
            }
            showNotification('Текст улучшен!', 'success');
        } else {
            showNotification('Ошибка обработки. Попробуйте позже', 'error');
        }
    } catch (error) {
        console.error('AI error:', error);
        showNotification('Ошибка соединения с сервером. Попробуйте позже.', 'error');
    }
}

document.addEventListener('DOMContentLoaded', function() {
    if (window.gamesData && Array.isArray(window.gamesData)) {
        allGames = window.gamesData;
    } else {
        console.error('gamesData is not defined or not an array');
    }

    if (window.tagsData && Array.isArray(window.tagsData)) {
        allTags = window.tagsData;

        const optionsContainer = document.getElementById('tagOptions');
        if (optionsContainer) {
            optionsContainer.innerHTML = '';
            allTags.forEach(tag => {
                const optionDiv = document.createElement('div');
                optionDiv.className = 'custom-select-option';
                optionDiv.setAttribute('data-tag-id', tag.id);
                optionDiv.setAttribute('data-tag-name', tag.name);
                optionDiv.innerHTML = `<span>${escapeHtml(tag.name)}</span>`;
                optionDiv.addEventListener('click', function(e) {
                    e.stopPropagation();
                    addTag({ id: tag.id, name: tag.name });
                    const dropdown = document.getElementById('tagDropdown');
                    if (dropdown) dropdown.style.display = 'none';
                });
                optionsContainer.appendChild(optionDiv);
            });
        }
    } else {
        console.error('tagsData is not defined or not an array');
    }

    initGameSearch();
    initCustomTagSelect();

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