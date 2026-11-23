/* Facilidades da tela de Configurações: busca, aviso de alterações não salvas,
   contador de caracteres, Ctrl+S e menu em lista no celular. */
(function () {
    function semAcento(t) { return (t || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase(); }

    // ---------- Menu: busca e lista no celular ----------
    var busca = document.getElementById('buscaMenu');
    var menu = document.querySelector('.config-menu');
    if (busca && menu) {
        busca.addEventListener('input', function () {
            var q = semAcento(busca.value.trim()), algum = false;
            menu.querySelectorAll('a').forEach(function (a) {
                var ok = !q || semAcento(a.textContent).indexOf(q) >= 0;
                a.hidden = !ok; if (ok) { algum = true; }
            });
            menu.querySelectorAll('.config-menu-titulo').forEach(function (t) { t.hidden = !!q; });
            document.getElementById('menuVazio').hidden = algum;
        });
        busca.addEventListener('keydown', function (e) {
            if (e.key === 'Enter') {
                var primeiro = menu.querySelector('a:not([hidden])');
                if (primeiro) { e.preventDefault(); primeiro.click(); }
            }
        });
    }
    var mobile = document.getElementById('menuMobile');
    if (mobile) { mobile.addEventListener('change', function () { window.location.href = mobile.value; }); }

    // ---------- Filtro de campos ----------
    var filtro = document.getElementById('filtroCampos');
    if (filtro) {
        filtro.addEventListener('input', function () {
            var q = semAcento(filtro.value.trim()), total = 0;
            document.querySelectorAll('.config-campo, .config-cor').forEach(function (c) {
                var ok = !q || semAcento(c.textContent + ' ' + (c.querySelector('input,textarea') || {}).value).indexOf(q) >= 0;
                c.hidden = !ok; if (ok) { total++; }
            });
            document.querySelectorAll('.config-grupo').forEach(function (g) {
                g.hidden = !g.querySelector('.config-campo:not([hidden])');
            });
            document.querySelectorAll('.config-grupo-cor').forEach(function (g) {
                var tem = !!g.querySelector('.config-cor:not([hidden])');
                g.hidden = !tem; if (q && tem) { g.open = true; }
            });
            document.getElementById('filtroVazio').hidden = total > 0;
        });
    }

    // ---------- Contador de caracteres ----------
    document.querySelectorAll('.js-texto[maxlength]').forEach(function (campo) {
        var limite = parseInt(campo.getAttribute('maxlength'), 10);
        if (!limite || limite <= 0) { return; }
        var cont = document.createElement('span');
        cont.className = 'config-contagem';
        campo.closest('.config-campo').appendChild(cont);
        function atualizar() {
            var n = campo.value.length;
            cont.textContent = n + ' / ' + limite;
            cont.classList.toggle('perto', n >= limite * 0.9);
        }
        campo.addEventListener('input', atualizar);
        atualizar();
    });

    // ---------- Alterações não salvas ----------
    var forms = [document.getElementById('formPersonalizacao'), document.getElementById('formAparencia')].filter(Boolean);
    forms.forEach(function (form) {
        var status = form.querySelector('[data-status]');
        var salvar = form.querySelector('button[type="submit"]:not([formaction])');
        var campos = Array.prototype.slice.call(form.querySelectorAll('input[name], textarea[name]'))
            .filter(function (c) { return c.type !== 'hidden'; });
        var inicial = new Map();
        campos.forEach(function (c) { inicial.set(c, c.type === 'file' ? '' : c.value); });
        var enviando = false;

        function pendentes() {
            return campos.filter(function (c) {
                var atual = c.type === 'file' ? (c.files.length ? 'x' : '') : c.value;
                return atual.trim() !== inicial.get(c).trim();
            }).length;
        }
        function atualizar() {
            var n = pendentes();
            form.classList.toggle('tem-pendencias', n > 0);
            if (status) {
                status.textContent = n === 0 ? 'Tudo salvo' : (n === 1 ? '1 alteração não salva' : n + ' alterações não salvas');
            }
            return n;
        }
        form.addEventListener('input', atualizar);
        form.addEventListener('change', atualizar);
        form.addEventListener('click', function () { setTimeout(atualizar, 0); }); // botões "Padrão" e amostras
        form.addEventListener('submit', function () {
            enviando = true;
            if (salvar) { salvar.disabled = true; salvar.textContent = 'Salvando…'; }
        });
        window.addEventListener('beforeunload', function (e) {
            if (!enviando && pendentes() > 0) { e.preventDefault(); e.returnValue = ''; }
        });
        document.addEventListener('keydown', function (e) {
            if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 's') {
                e.preventDefault();
                if (salvar && !salvar.disabled) { form.requestSubmit ? form.requestSubmit(salvar) : salvar.click(); }
            }
        });
        if (salvar) { salvar.title = 'Atalho: Ctrl+S'; }
        atualizar();
    });
})();
