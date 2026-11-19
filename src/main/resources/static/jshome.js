// ===== Sidebar esquerda =====
const menuBtn = document.getElementById('menuBtn');
const sidebar = document.getElementById('sidebar');
const overlay = document.getElementById('overlay');

function fecharSidebar() {
    sidebar?.classList.remove('active');
    overlay?.classList.remove('active');
}

function toggleSidebar() {
    // Fecha o carrinho se estiver aberto para não sobrepor menus
    window.EstiloLondonCarrinho?.fechar();
    sidebar?.classList.toggle('active');
    overlay?.classList.toggle('active');
}

if (menuBtn) menuBtn.addEventListener('click', toggleSidebar);
if (overlay) overlay.addEventListener('click', fecharSidebar);

// ===== Busca local nos cards (quando não é data-servidor) =====
const searchInput = document.querySelector('input[name="search"]');
const searchForm = document.querySelector('.search-form');
const buscaNoServidor = searchForm && searchForm.hasAttribute('data-servidor');

const produtos = document.querySelectorAll('.produto-card');

if (searchInput && !buscaNoServidor) {
    searchInput.addEventListener('input', function () {
        const termoBusca = searchInput.value.toLowerCase();

        produtos.forEach(produto => {
            const tituloProduto = produto.querySelector('h3').innerText.toLowerCase();

            if (tituloProduto.includes(termoBusca)) {
                produto.style.display = 'block';
            } else {
                produto.style.display = 'none';
            }
        });
    });
}

if (searchForm && !buscaNoServidor) {
    searchForm.addEventListener('submit', function (event) {
        event.preventDefault();
    });
}

// ===== Barra de anúncios: troca a mensagem a cada 4 segundos =====
(function () {
    const mensagens = document.querySelectorAll('.barra-anuncios .anuncio');
    const barra = document.querySelector('.barra-anuncios');
    if (!barra || mensagens.length < 2) return;

    let atual = 0;
    let timer = null;

    mensagens.forEach(function (m, i) {
        m.setAttribute('aria-hidden', i === 0 ? 'false' : 'true');
    });

    function proxima() {
        mensagens[atual].classList.remove('ativo');
        mensagens[atual].setAttribute('aria-hidden', 'true');
        atual = (atual + 1) % mensagens.length;
        mensagens[atual].classList.add('ativo');
        mensagens[atual].setAttribute('aria-hidden', 'false');
    }

    function iniciar() { timer = setInterval(proxima, 4000); }
    function pausar() { clearInterval(timer); }

    barra.addEventListener('mouseenter', pausar);
    barra.addEventListener('mouseleave', iniciar);
    iniciar();
})();

// ===== Tela de Detalhes: Galeria, Modal de Zoom e Seleção de Tamanhos =====
(function () {
    const principal = document.getElementById('imagemPrincipal');
    if (!principal) return; // Só roda na página de detalhes

    const legenda = document.getElementById('legendaImagem');
    const tira = document.getElementById('galeriaMiniaturas');
    const caixa = document.querySelector('.detalhes-box');
    const semFoto = principal.getAttribute('data-sem-foto');
    const nome = principal.getAttribute('data-nome') || 'Produto';
    let minis = tira ? Array.from(tira.querySelectorAll('.detalhes-mini')) : [];

    function mostrar(mini) {
        const src = mini ? mini.getAttribute('data-src') : semFoto;
        if (principal.getAttribute('src') !== src) {
            principal.classList.add('trocando');
            principal.setAttribute('src', src);
        }
        principal.alt = mini ? `${nome} - ${mini.getAttribute('data-rotulo')}` : `${nome} (sem foto disponível)`;
        if (legenda) {
            legenda.textContent = mini ? mini.getAttribute('data-rotulo') : '';
            legenda.hidden = !mini;
        }
    }

    function ativar(mini) {
        minis.forEach((m) => {
            const ligada = m === mini;
            m.classList.toggle('ativa', ligada);
            m.setAttribute('aria-pressed', ligada ? 'true' : 'false');
        });
        mostrar(mini);
    }

    function descartar(mini) {
        const eraAtiva = mini.classList.contains('ativa');
        minis = minis.filter((m) => m !== mini);
        mini.remove();
        if (minis.length < 2 && tira) {
            tira.remove();
            caixa?.classList.add('sem-miniaturas');
        }
        if (eraAtiva || minis.length === 0) {
            ativar(minis[0] || null);
        }
    }

    minis.forEach((mini) => {
        mini.addEventListener('click', () => ativar(mini));
        const teste = new Image();
        teste.onerror = () => descartar(mini);
        teste.src = mini.getAttribute('data-src');
    });

    if (tira) {
        tira.addEventListener('keydown', (e) => {
            const atual = minis.indexOf(document.activeElement);
            if (atual < 0) return;

            let destino = -1;
            if (e.key === 'ArrowRight' || e.key === 'ArrowDown') destino = (atual + 1) % minis.length;
            else if (e.key === 'ArrowLeft' || e.key === 'ArrowUp') destino = (atual - 1 + minis.length) % minis.length;
            else if (e.key === 'Home') destino = 0;
            else if (e.key === 'End') destino = minis.length - 1;

            if (destino < 0) return;
            e.preventDefault();
            minis[destino].focus();
            ativar(minis[destino]);
        });
    }

    // Modal de zoom
    const modal = document.getElementById('zoomModal');
    const zImg = document.getElementById('zoomImg');
    const zLeg = document.getElementById('zoomLegenda');
    const zAnt = document.getElementById('zoomAnt');
    const zProx = document.getElementById('zoomProx');
    const btnAmpliar = document.getElementById('btnAmpliar');
    let ultimoFoco = null;

    function indiceAtivo() {
        const i = minis.findIndex((m) => m.classList.contains('ativa'));
        return i < 0 ? 0 : i;
    }

    function atualizarZoom() {
        const mini = minis[indiceAtivo()];
        zImg.src = mini ? mini.getAttribute('data-src') : principal.getAttribute('src');
        zImg.alt = mini ? `${nome} - ${mini.getAttribute('data-rotulo')}` : nome;
        zLeg.textContent = mini ? mini.getAttribute('data-rotulo') : '';
        const navegavel = minis.length > 1;
        zAnt.hidden = zProx.hidden = !navegavel;
    }

    function abrirZoom() {
        ultimoFoco = document.activeElement;
        atualizarZoom();
        modal.hidden = false;
        document.body.classList.add('zoom-aberto');
        document.getElementById('zoomFechar')?.focus();
    }

    function fecharZoom() {
        modal.hidden = true;
        zImg.removeAttribute('src');
        document.body.classList.remove('zoom-aberto');
        if (ultimoFoco?.focus) ultimoFoco.focus();
    }

    function navegarZoom(passo) {
        if (minis.length < 2) return;
        ativar(minis[(indiceAtivo() + passo + minis.length) % minis.length]);
        atualizarZoom();
    }

    if (modal) {
        btnAmpliar?.addEventListener('click', abrirZoom);
        principal.addEventListener('click', () => {
            if (btnAmpliar) abrirZoom();
        });
        document.getElementById('zoomFechar')?.addEventListener('click', fecharZoom);
        zAnt?.addEventListener('click', () => navegarZoom(-1));
        zProx?.addEventListener('click', () => navegarZoom(1));
        modal.addEventListener('click', (e) => {
            if (e.target === modal) fecharZoom();
        });
        document.addEventListener('keydown', (e) => {
            if (modal.hidden) return;
            if (e.key === 'Escape') fecharZoom();
            else if (e.key === 'ArrowRight') navegarZoom(1);
            else if (e.key === 'ArrowLeft') navegarZoom(-1);
        });
    }

    principal.addEventListener('load', () => principal.classList.remove('trocando'));
    principal.addEventListener('error', () => {
        principal.classList.remove('trocando');
        if (principal.getAttribute('src') !== semFoto) {
            principal.setAttribute('src', semFoto);
            principal.alt = `${nome} (sem foto disponível)`;
            if (legenda) legenda.hidden = true;
        }
    });

    // Seleção de tamanhos e sincronização com botões de compra
    const botoesTamanho = document.querySelectorAll('.detalhes-tamanhos button');
    const botoesCompra = document.querySelectorAll('.detalhes-compra .js-add-carrinho');

    function sincronizarTamanho(tam) {
        botoesCompra.forEach((btn) => btn.setAttribute('data-tamanho', tam));
    }

    botoesTamanho.forEach((btn) => {
        btn.addEventListener('click', () => {
            botoesTamanho.forEach((b) => {
                b.classList.remove('ativo');
                b.setAttribute('aria-checked', 'false');
            });
            btn.classList.add('ativo');
            btn.setAttribute('aria-checked', 'true');
            sincronizarTamanho(btn.getAttribute('data-tamanho') || btn.textContent.trim());
        });
    });

    const ativoInicial = document.querySelector('.detalhes-tamanhos button.ativo');
    if (ativoInicial) {
        sincronizarTamanho(ativoInicial.getAttribute('data-tamanho') || ativoInicial.textContent.trim());
    }
})();

// ===== Lista de desejos (favoritos): coração nos cards e nos detalhes, guardado no localStorage =====
(function () {
    const CHAVE = 'estilo_london_wishlist_v1';
    const ROTULO_ADD = 'Adicionar aos favoritos';
    const ROTULO_REM = 'Remover dos favoritos';

    function ler() {
        try {
            const bruto = localStorage.getItem(CHAVE);
            const lista = bruto ? JSON.parse(bruto) : [];
            return Array.isArray(lista) ? lista : [];
        } catch (e) {
            return [];
        }
    }

    function salvar(itens) {
        try {
            localStorage.setItem(CHAVE, JSON.stringify(itens));
        } catch (e) { /* modo privado / storage cheio: o coração só não persiste */ }
    }

    // Pinta todos os corações da página conforme a lista salva e atualiza o contador do topo
    function sincronizar() {
        const itens = ler();
        const ids = new Set(itens.map((i) => String(i.id)));

        document.querySelectorAll('.js-btn-favorito').forEach((btn) => {
            const dono = btn.closest('[data-produto-id]');
            if (!dono) return;
            const ativo = ids.has(String(dono.getAttribute('data-produto-id')));
            const nome = dono.getAttribute('data-produto-nome');
            const rotulo = (ativo ? ROTULO_REM : ROTULO_ADD) + (nome ? ': ' + nome : '');
            btn.classList.toggle('ativo', ativo);
            btn.setAttribute('aria-pressed', ativo ? 'true' : 'false');
            btn.setAttribute('aria-label', rotulo);
            btn.title = ativo ? ROTULO_REM : ROTULO_ADD;
        });

        const contador = document.getElementById('wishlistQtdTotal');
        if (contador) contador.textContent = String(ids.size);
    }

    function alternar(dono) {
        const id = String(dono.getAttribute('data-produto-id'));
        const itens = ler();
        const pos = itens.findIndex((i) => String(i.id) === id);
        if (pos > -1) {
            itens.splice(pos, 1);
        } else {
            itens.push({
                id: id,
                nome: dono.getAttribute('data-produto-nome') || 'Produto',
                preco: dono.getAttribute('data-produto-preco') || '0',
                imagem: dono.getAttribute('data-produto-imagem') || '',
                link: dono.getAttribute('data-produto-link') || ('/detalhes/' + id)
            });
        }
        salvar(itens);
        sincronizar();
        document.dispatchEvent(new CustomEvent('wishlist:alterada', { detail: { id: id, ativo: pos === -1 } }));
    }

    // ---------- Página de favoritos: desenha a lista salva no mesmo formato dos cards da vitrine ----------
    const SVG_CORACAO = '<svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/></svg>';
    const SEM_FOTO = '/img/sem-foto.svg';

    function esc(t) {
        return String(t == null ? '' : t).replace(/[&<>"']/g, (c) =>
            ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
    }

    function linkDe(item) {
        // só caminhos internos (o localStorage pode ser editado pelo usuário)
        return typeof item.link === 'string' && item.link.startsWith('/') && !item.link.startsWith('//')
            ? item.link : '/detalhes/' + encodeURIComponent(item.id);
    }

    function precoBR(valor) {
        const n = Number(valor);
        return 'R$ ' + (isFinite(n) ? n : 0).toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    }

    function htmlCard(item) {
        const link = esc(linkDe(item));
        return '<div class="produto-card" data-produto-id="' + esc(item.id) + '" data-produto-nome="' + esc(item.nome) + '"'
            + ' data-produto-preco="' + esc(item.preco) + '" data-produto-imagem="' + esc(item.imagem) + '" data-produto-link="' + link + '">'
            + '<div class="produto-media-box">'
            + '<a class="produto-media" href="' + link + '"><img class="img-frente" src="' + esc(item.imagem || SEM_FOTO)
            + '" alt="' + esc(item.nome) + '" loading="lazy"></a>'
            + '<button type="button" class="btn-favorito js-btn-favorito ativo" aria-pressed="true">' + SVG_CORACAO + '</button>'
            + '</div>'
            + '<h3><a class="produto-titulo" href="' + link + '">' + esc(item.nome) + '</a></h3>'
            + '<p class="preco"><span>' + precoBR(item.preco) + '</span></p>'
            + '</div>';
    }

    /**
     * Lista os favoritos dentro de `grade` (use a classe .produtos-grid) e mostra `vazio` quando não há nenhum.
     * Reage sozinho a mudanças: desmarcar o coração tira o card; outra aba também atualiza.
     */
    function renderizar(grade, vazio) {
        if (!grade) return;

        function mostrarEstado() {
            const temItens = grade.querySelector('.produto-card') !== null;
            grade.style.display = temItens ? '' : 'none';
            if (vazio) vazio.hidden = temItens;
        }

        function desenhar() {
            grade.innerHTML = ler().map(htmlCard).join('');
            grade.querySelectorAll('img').forEach((img) => {
                img.addEventListener('error', () => { img.src = SEM_FOTO; }, { once: true });
            });
            mostrarEstado();
            sincronizar();
        }

        document.addEventListener('wishlist:alterada', (e) => {
            if (e.detail.ativo) return;
            grade.querySelectorAll('.produto-card').forEach((card) => {
                if (card.getAttribute('data-produto-id') === String(e.detail.id)) card.remove();
            });
            mostrarEstado();
        });
        window.addEventListener('storage', (e) => { if (e.key === CHAVE) desenhar(); });

        desenhar();
    }

    // Um único listener (delegação): vale para os cards da vitrine, dos detalhes e da página de favoritos
    document.addEventListener('click', (e) => {
        const btn = e.target.closest('.js-btn-favorito');
        if (!btn) return;
        e.preventDefault();
        e.stopPropagation();
        const dono = btn.closest('[data-produto-id]');
        if (!dono) return;
        alternar(dono);
        btn.classList.remove('pulso');
        void btn.offsetWidth; // reinicia a animação
        btn.classList.add('pulso');
    });

    // Mantém várias abas / voltar do histórico em sincronia
    window.addEventListener('storage', (e) => { if (e.key === CHAVE) sincronizar(); });
    window.addEventListener('pageshow', sincronizar);

    sincronizar();
    window.EstiloLondonWishlist = { ler: ler, sincronizar: sincronizar, renderizar: renderizar };
})();
