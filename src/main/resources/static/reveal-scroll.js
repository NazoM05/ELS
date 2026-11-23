/* Revelação progressiva e repetida dos blocos conforme entram/saem da área visível. */
(function () {
    'use strict';

    var reduzirMovimento = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (reduzirMovimento || !('IntersectionObserver' in window)) return;

    function iniciar() {
        /* Painel admin não usa animação de revelação. */
        if (document.body.classList.contains('admin-ui')) return;
        /* Na Home, a entrada/saída por rolagem não deve ser aplicada aos produtos.
           No máximo, o banner pode receber a revelação. As demais páginas
           continuam usando a lista completa abaixo. */
        var seletores = document.body.classList.contains('pg-inicio')
            ? ['.pg-inicio .promo-miami', '.pg-inicio .banner-card']
            : document.body.classList.contains('pg-london')
            ? ['.pg-london .promo-miami', '.pg-london .banner-card']
            : [
            'main > section', 'main > article', 'main > .container > section',
            '.pg-london > section', '.pg-london > .container-produtos',
            '.secao-titulo', '.produto-card', '.categoria-card', '.colecao-card',
            '.banner-card', '.promo-miami', '.institucional-pagina .inst-topic-mark',
            '.institucional-pagina .inst-hero > *',
            '.institucional-pagina .inst-feature-card', '.institucional-pagina .inst-policy-card',
            '.institucional-pagina .inst-summary-card', '.institucional-pagina .inst-info-panel',
            '.institucional-pagina .inst-step', '.institucional-pagina .inst-benefit',
            '.institucional-pagina .inst-privacy-cards > div',
            '.institucional-pagina .inst-rights-grid > div',
            '.institucional-pagina .inst-check-grid > div',
            '.institucional-pagina .inst-refund-grid > div',
            '.institucional-pagina .inst-privacy-split > div',
            '.institucional-pagina .inst-retention-grid > div',
            '.institucional-pagina .inst-third-party-list > div',
            '.institucional-pagina .inst-bottom-note',
            '.checkout-section', '.checkout-card', '.perfil-card', '.pedido-card',
            '.el-cart-drawer .el-cart-header'
        ];

        var elementos = document.querySelectorAll(seletores.join(','));
        if (!elementos.length) return;

        /* Cada elemento recebe um pequeno atraso relativo ao grupo em que está.
           A animação é controlada pelo CSS para não alterar o layout. */
        elementos.forEach(function (elemento, indice) {
            if (elemento.closest('[hidden], [aria-hidden="true"]')) return;
            elemento.classList.add('reveal-scroll');
            elemento.style.setProperty('--reveal-delay', Math.min((indice % 6) * 55, 275) + 'ms');
        });

        var observador = new IntersectionObserver(function (entradas) {
            entradas.forEach(function (entrada) {
                var elemento = entrada.target;

                if (entrada.isIntersecting) {
                    /* Entrando na viewport: revela. */
                    elemento.classList.add('reveal-visivel');
                } else {
                    /* Saindo da viewport: prepara novamente.
                       Assim, ao voltar a rolar para o elemento, a animação
                       acontece outra vez na mesma página. */
                    elemento.classList.remove('reveal-visivel');
                }
            });
        }, {
            root: null,
            rootMargin: '0px 0px -10% 0px',
            threshold: 0.08
        });

        elementos.forEach(function (elemento) {
            if (!elemento.closest('[hidden], [aria-hidden="true"]')) {
                observador.observe(elemento);
            }
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', iniciar, { once: true });
    } else {
        iniciar();
    }
})();
