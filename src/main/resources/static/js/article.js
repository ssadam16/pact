document.addEventListener('DOMContentLoaded', function() {
    const images = document.querySelectorAll('.article-content img');
    images.forEach(img => {
        img.setAttribute('loading', 'lazy');

        /*404*/
        /*img.addEventListener('error', function() {
            this.style.display = 'none';
        });*/
    });

    /*новая вкладка для внешних ссылок*/
    /*const links = document.querySelectorAll('.article-content a');
    links.forEach(link => {
        if (link.hostname !== window.location.hostname) {
            link.setAttribute('target', '_blank');
            link.setAttribute('rel', 'noopener noreferrer');
        }
    });*/
});