const uploadUrl = '/api/images/upload';

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
                headers: {
                    [getCsrfHeader()]: getCsrfToken()
                }
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

    setTimeout(() => {
        alertDiv.remove();
    }, 5000);
}

function submitArticle() {
    const content = quill.root.innerHTML;
    const hiddenContent = document.getElementById('content');

    if (hiddenContent) {
        hiddenContent.value = content;
    }

    const title = document.querySelector('[name="title"]')?.value;

    if (!title || title.trim() === '') {
        showNotification('Введите название статьи', 'error');
        return;
    }

    const emptyContent = content === '<p><br></p>' || content === '' || content === '<p></p>';
    if (emptyContent) {
        showNotification('Заполните содержание статьи', 'error');
        return;
    }

    document.getElementById('articleForm').submit();
}