// Mapa do Brasil na página Frete e Entrega: destaca o estado e mostra o prazo de entrega.
// Os prazos vêm da tabela "Ver todos os prazos" (gerada pelo servidor), então não há dados duplicados aqui.
(function () {
    const raiz = document.querySelector('[data-mapa-frete]');
    if (!raiz) return;

    const prazos = {};
    document.querySelectorAll('.mapa-frete-lista tr[data-uf]').forEach(function (linha) {
        prazos[linha.dataset.uf] = {
            nome: linha.dataset.nome,
            regiao: linha.dataset.regiao,
            min: linha.dataset.min,
            max: linha.dataset.max
        };
    });

    const ufsSul = (raiz.dataset.ufsSul || '').split(',');
    const info = {
        regiao: raiz.querySelector('[data-info-regiao]'),
        nome: raiz.querySelector('[data-info-nome]'),
        prazoBloco: raiz.querySelector('[data-info-prazo-bloco]'),
        prazo: raiz.querySelector('[data-info-prazo]'),
        dica: raiz.querySelector('[data-info-dica]'),
        frete: raiz.querySelector('[data-info-frete]')
    };
    const estados = raiz.querySelectorAll('.mapa-estado');
    let fixado = null; // estado escolhido por clique/toque/Enter

    function mostrar(uf) {
        estados.forEach(function (el) { el.classList.toggle('ativo', el.dataset.uf === uf); });
        raiz.querySelectorAll('.mapa-rotulo').forEach(function (el) { el.classList.toggle('ativo', el.dataset.uf === uf); });

        const p = uf && prazos[uf];
        if (!p) {
            info.regiao.textContent = 'Prazo de entrega';
            info.nome.textContent = 'Escolha um estado';
            info.prazoBloco.hidden = true;
            info.frete.hidden = true;
            info.dica.hidden = false;
            return;
        }
        info.regiao.textContent = 'Região ' + p.regiao;
        info.nome.textContent = p.nome;
        info.prazo.textContent = p.min + ' a ' + p.max;
        info.prazoBloco.hidden = false;
        info.dica.hidden = true;
        const minimo = ufsSul.includes(uf) ? raiz.dataset.sul : raiz.dataset.demais;
        info.frete.textContent = 'Frete grátis a partir de R$\u00a0' + minimo;
        info.frete.hidden = false;
    }

    estados.forEach(function (el) {
        const uf = el.dataset.uf;
        el.addEventListener('pointerenter', function (e) { if (e.pointerType === 'mouse') mostrar(uf); });
        el.addEventListener('focus', function () { mostrar(uf); });
        el.addEventListener('click', function () { fixado = uf; mostrar(uf); });
        el.addEventListener('keydown', function (e) {
            if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); fixado = uf; mostrar(uf); }
        });
    });

    const svg = raiz.querySelector('.mapa-brasil-svg');
    // Ao sair do mapa, volta para o estado escolhido por clique (ou para o aviso inicial)
    svg.addEventListener('pointerleave', function (e) { if (e.pointerType === 'mouse') mostrar(fixado); });
    svg.addEventListener('focusout', function (e) { if (!svg.contains(e.relatedTarget)) mostrar(fixado); });
})();
