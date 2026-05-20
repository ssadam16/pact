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

    const toggleButtons = document.querySelectorAll('.password-toggle');
    toggleButtons.forEach(btn => {
        btn.addEventListener('click', function() {
            const input = this.closest('.input-group').querySelector('input');
            const type = input.getAttribute('type') === 'password' ? 'text' : 'password';
            input.setAttribute('type', type);
            const icon = this.querySelector('i');
            icon.classList.toggle('bi-eye');
            icon.classList.toggle('bi-eye-slash');
        });
    });
});