document.addEventListener('DOMContentLoaded', function() {
    const logo = document.getElementById('logoLink');
    if (logo) {
        let clickCount = 0;
        let timeoutId = null;

        logo.addEventListener('click', function(e) {
            e.preventDefault();
            clickCount++;

            if (timeoutId) {
                clearTimeout(timeoutId);
            }

            timeoutId = setTimeout(() => {
                clickCount = 0;
                window.location.href = '/';
            }, 1000);

            if (clickCount === 7) {
                window.location.href = 'egg/scp';
                clickCount = 0;
                if (timeoutId) {
                    clearTimeout(timeoutId);
                }
            }
        });
    }
});