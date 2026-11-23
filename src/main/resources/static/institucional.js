// Navegação, animações progressivas e pequenos recursos de leitura das páginas institucionais.
(function () {
    const page = document.querySelector('.institucional-pagina');
    const menu = document.querySelector('.institucional-menu');
    const texto = document.querySelector('.institucional-texto');
    if (!page || !texto) return;

    const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)');
    const progress = document.createElement('div');
    progress.className = 'inst-progress';
    progress.setAttribute('aria-hidden', 'true');
    progress.innerHTML = '<span></span>';
    page.appendChild(progress);
    const progressFill = progress.firstElementChild;

    const topButton = document.createElement('button');
    topButton.className = 'inst-top-button';
    topButton.type = 'button';
    topButton.setAttribute('aria-label', 'Voltar ao topo');
    topButton.innerHTML = '↑';
    page.appendChild(topButton);
    topButton.addEventListener('click', function () {
        window.scrollTo({ top: 0, behavior: reduceMotion.matches ? 'auto' : 'smooth' });
    });

    function updateScrollUI() {
        const max = document.documentElement.scrollHeight - window.innerHeight;
        const ratio = max > 0 ? Math.min(1, Math.max(0, window.scrollY / max)) : 0;
        progressFill.style.width = (ratio * 100) + '%';
        topButton.classList.toggle('visivel', window.scrollY > 420);
    }
    window.addEventListener('scroll', updateScrollUI, { passive: true });
    window.addEventListener('resize', updateScrollUI);
    updateScrollUI();

    // Revela blocos conforme entram na área visível; fallback sem IntersectionObserver.
    const revealItems = texto.querySelectorAll('.inst-hero, .inst-page-head, .inst-section, .inst-contact-panel, .inst-shipping-summary, .inst-privacy-intro, .inst-toc, .inst-privacy-section, .inst-bottom-note');
    if (!reduceMotion.matches && 'IntersectionObserver' in window) {
        const observer = new IntersectionObserver(function (entries, obs) {
            entries.forEach(function (entry) {
                if (entry.isIntersecting) {
                    entry.target.classList.add('inst-visible');
                    obs.unobserve(entry.target);
                }
            });
        }, { threshold: 0.12, rootMargin: '0px 0px -36px 0px' });
        revealItems.forEach(function (el, index) {
            el.classList.add('inst-reveal');
            el.dataset.instDelay = String(index % 4);
            observer.observe(el);
        });
    } else {
        revealItems.forEach(function (el) { el.classList.add('inst-visible'); });
    }

    // Marca o tópico de privacidade que está mais próximo do topo da leitura.
    const tocLinks = Array.from(texto.querySelectorAll('.inst-toc a[href^="#"]'));
    const tocSections = tocLinks.map(function (link) {
        return document.querySelector(link.getAttribute('href'));
    }).filter(Boolean);
    if (tocSections.length && 'IntersectionObserver' in window) {
        const tocObserver = new IntersectionObserver(function (entries) {
            entries.forEach(function (entry) {
                if (!entry.isIntersecting) return;
                tocLinks.forEach(function (link) {
                    if (link.getAttribute('href') === '#' + entry.target.id) {
                        link.setAttribute('aria-current', 'location');
                    } else {
                        link.removeAttribute('aria-current');
                    }
                });
            });
        }, { rootMargin: '-18% 0px -68% 0px', threshold: 0 });
        tocSections.forEach(function (section) { tocObserver.observe(section); });
    }

    // Transição curta entre as páginas institucionais; mantém Ctrl/Cmd+clique e nova aba nativos.
    if (!menu) return;
    const EXIT_MS = 160;
    menu.addEventListener('click', function (e) {
        const link = e.target.closest('a[href]');
        if (!link || e.defaultPrevented || e.button !== 0 || e.metaKey || e.ctrlKey || e.shiftKey || e.altKey) return;
        if ((link.target && link.target !== '_self') || link.origin !== window.location.origin) return;
        if (link.classList.contains('atual')) {
            e.preventDefault();
            window.scrollTo({ top: 0, behavior: reduceMotion.matches ? 'auto' : 'smooth' });
            return;
        }
        if (reduceMotion.matches) return;
        e.preventDefault();
        texto.classList.add('saindo');
        window.setTimeout(function () { window.location.href = link.href; }, EXIT_MS);
    });
    window.addEventListener('pageshow', function (e) {
        if (e.persisted) texto.classList.remove('saindo');
    });
})();
