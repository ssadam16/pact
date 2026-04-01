document.addEventListener('DOMContentLoaded', function() {
    const glow = document.getElementById('dynamicGlow');
    if (glow) {
        const hour = new Date().getHours();
        let gradientStart, gradientEnd, opacityValue;

        if (hour >= 6 && hour < 12) {
            gradientStart = '#a855f7';
            gradientEnd = '#f97316';
            opacityValue = 0.28;
        } else if (hour >= 12 && hour < 19) {
            gradientStart = '#8b5cf6';
            gradientEnd = '#06b6d4';
            opacityValue = 0.4;
        } else {
            gradientStart = '#6b21a5';
            gradientEnd = '#155e75';
            opacityValue = 0.45;
        }

        glow.style.background = `radial-gradient(circle at 35% 40%, ${gradientStart}, ${gradientEnd})`;
        glow.style.opacity = opacityValue;

        const sphere = document.getElementById('pactSphere');
        let mouseX = 0, mouseY = 0;
        let currentX = 0, currentY = 0;

        function animateGlow() {
            currentX += (mouseX - currentX) * 0.08;
            currentY += (mouseY - currentY) * 0.08;
            if (glow) {
                glow.style.transform = `translate(${currentX * 12}px, ${currentY * 12}px)`;
            }
            requestAnimationFrame(animateGlow);
        }

        if (sphere) {
            sphere.addEventListener('mousemove', (e) => {
                const rect = sphere.getBoundingClientRect();
                mouseX = (e.clientX - rect.left) / rect.width - 0.5;
                mouseY = (e.clientY - rect.top) / rect.height - 0.5;
            });
            sphere.addEventListener('mouseleave', () => {
                mouseX = 0;
                mouseY = 0;
            });
            animateGlow();
        }
    }
});