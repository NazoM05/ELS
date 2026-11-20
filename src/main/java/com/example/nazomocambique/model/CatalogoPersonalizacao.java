package com.example.nazomocambique.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lista fechada de tudo o que a tela Configurações deixa personalizar: textos (nome das páginas, botões,
 * títulos...) e cores de cada item da interface, para o site todo e para cada página.
 *
 * Só as chaves daqui são aceitas ao salvar, e os seletores de CSS são fixos no código: o que vem do
 * formulário é só o texto (impresso com escape) ou a cor (validada como #RRGGBB).
 *
 * Chaves:
 *   geral.*                 texto que aparece no site todo (cabeçalho, menu, carrinho, rodapé)
 *   pg.&lt;pagina&gt;.*           texto de uma página
 *   cor.&lt;item&gt;              cor de um item em todas as páginas
 *   pg.&lt;pagina&gt;.cor.&lt;item&gt;  cor de um item só naquela página (vence a de todas as páginas)
 */
public final class CatalogoPersonalizacao {

	/** Um texto editável. {@code opcional}: pode ficar vazio (e some do site); senão, vazio volta ao padrão. */
	public record Campo(String chave, String rotulo, String padrao, boolean longo, boolean opcional) {
		public int limite() {
			return longo ? 600 : 200;
		}
	}

	public record Grupo(String nome, List<Campo> campos) {
	}

	/**
	 * Cor de um item da interface. Com {@code variavel}, muda a variável do tema (e tudo o que a usa);
	 * senão, aplica as {@code propriedades} nos elementos do {@code seletor}.
	 */
	public record ItemCor(String id, String grupo, String rotulo, String seletor, List<String> propriedades,
			String variavel, boolean hover, boolean somentePagina) {
		public boolean isVariavel() {
			return variavel != null;
		}
	}

	public record GrupoCores(String nome, List<ItemCor> itens) {
	}

	/** Página do site. {@code urlPrevia} é aberta na prévia da tela de configurações (null = sem prévia). */
	public record Pagina(String id, String nome, String descricao, String urlPrevia, List<Grupo> grupos) {
		public String classeCss() {
			return "pg-" + id;
		}

		public String escopo() {
			return "body." + classeCss();
		}
	}

	/** Item do menu da tela de configurações. tipo: aparencia | textos | cores | pagina */
	public record Secao(String id, String nome, String tipo, String descricao, Pagina pagina) {
	}

	public static final String ESCOPO_GERAL = "body";

	private static final List<Grupo> TEXTOS_GERAIS;
	private static final List<ItemCor> CORES;
	private static final List<Pagina> PAGINAS;
	private static final List<Secao> SECOES;
	private static final Map<String, Campo> CAMPOS = new LinkedHashMap<>();
	private static final Map<String, ItemCor> CORES_POR_ID = new LinkedHashMap<>();
	private static final Map<String, Pagina> PAGINAS_POR_ID = new LinkedHashMap<>();
	private static final Map<String, Secao> SECOES_POR_ID = new LinkedHashMap<>();

	static {
		TEXTOS_GERAIS = List.of(
				grupo("Marca",
						t("geral.nomeLoja", "Nome da loja (usado nas abas do navegador)", "Estilo London Store"),
						t("geral.nomeCurto", "Nome curto (logo em texto do login e do rodapé)", "Estilo London")),
				grupo("Barra de avisos (deixe em branco para esconder)",
						opc("geral.anuncio1", "Aviso 1", "Frete grátis a partir de R$ 99 para a região Sul"),
						opc("geral.anuncio2", "Aviso 2", "Novas estampas disponíveis"),
						opc("geral.anuncio3", "Aviso 3", "Parcele em até 3x sem juros em toda a loja"),
						opc("geral.anuncio4", "Aviso 4", "Troca facilitada em até 30 dias")),
				grupo("Topo e busca",
						t("geral.buscaPlaceholder", "Texto dentro do campo de busca", "Pesquisar..."),
						t("geral.buscaBotao", "Botão da busca", "Buscar")),
				grupo("Menu lateral",
						t("geral.menuInicio", "Link: início", "Início"),
						t("geral.menuPerfil", "Link: perfil", "Perfil"),
						t("geral.menuAdminTitulo", "Título da área do administrador", "Opções de administrador"),
						t("geral.menuConfiguracoes", "Link: configurações", "Configurações"),
						t("geral.menuPedidos", "Link: pedidos", "Pedidos"),
						t("geral.menuProdutos", "Link: gerenciar produtos", "Gerenciar Produtos"),
						t("geral.menuEntrar", "Link: entrar", "Entrar"),
						t("geral.menuCriarConta", "Link: criar conta", "Criar conta"),
						t("geral.menuSair", "Link: sair", "Sair")),
				grupo("Carrinho",
						t("geral.carrinhoTitulo", "Título do carrinho", "Seu Carrinho"),
						t("geral.carrinhoSubtotal", "Rótulo do subtotal", "Subtotal"),
						t("geral.carrinhoFinalizar", "Botão finalizar compra", "Finalizar compra"),
						t("geral.carrinhoLimpar", "Botão limpar carrinho", "Limpar carrinho"),
						t("geral.carrinhoRemover", "Botão remover item", "Remover"),
						t("geral.carrinhoVazio", "Mensagem de carrinho vazio", "Seu carrinho está vazio.")),
				grupo("Catálogo (filtros, cards e paginação)",
						t("geral.filtroGenero", "Rótulo do filtro de gênero", "Gênero"),
						t("geral.filtroTodos", "Opção todos os gêneros", "Todos"),
						t("geral.filtroCategoria", "Rótulo do filtro de categoria", "Categoria"),
						t("geral.filtroTodasCategorias", "Opção todas as categorias", "Todas as categorias"),
						t("geral.filtroLimpar", "Link limpar filtros", "Limpar filtros"),
						t("geral.filtroVazio", "Mensagem sem resultados", "Nenhum produto encontrado com esses filtros."),
						t("geral.cardParcelamento", "Texto do parcelamento nos cards (antes do valor)", "Até 12x de"),
						t("geral.paginaAnterior", "Botão página anterior", "Anterior"),
						t("geral.paginaProxima", "Botão próxima página", "Próxima")),
				grupo("Rodapé",
						t("geral.rodapeInstitucional", "Título da coluna institucional", "Institucional"),
						t("geral.rodapeSobre", "Link: sobre nós", "Sobre nós"),
						t("geral.rodapeFrete", "Link: frete e entrega", "Frete e Entrega"),
						t("geral.rodapeTrocas", "Link: trocas e devoluções", "Trocas e Devoluções"),
						t("geral.rodapePrivacidade", "Link: política de privacidade", "Política de Privacidade"),
						t("geral.rodapeAtendimento", "Título do atendimento", "Atendimento"),
						opc("geral.rodapeWhatsapp", "Texto do WhatsApp", "WhatsApp (SAC)"),
						opc("geral.rodapeEmail", "E-mail de contato", "contato@seudominio.com.br"),
						t("geral.rodapeColecoes", "Título da coluna de coleções", "Coleções"),
						t("geral.rodapeNovidades", "Link: novidades", "Novidades"),
						t("geral.rodapeCamisetas", "Link: camisetas", "Camisetas"),
						t("geral.rodapeOversized", "Link: oversized", "Oversized"),
						t("geral.rodapeCalcas", "Link: calças", "Calças"),
						t("geral.rodapeAcessorios", "Link: acessórios", "Acessórios"),
						t("geral.rodapeOutlet", "Link: outlet", "Outlet"),
						opc("geral.rodapeCopyright", "Linha de direitos / CNPJ", "© 2026 Estilo London Store. CNPJ: 00.000.000/0001-00."),
						opc("geral.rodapeEndereco", "Endereço", "Rua Exemplo, 123 - Cidade/UF | CEP 00000-000")));

		CORES = List.of(
				// Variáveis do tema: mudam tudo o que usa aquela cor
				variavel("principal", "Página", "Cor principal (botões, avisos, menu e rodapé desta página)", "--primary-color", true),
				variavel("fundo", "Página", "Fundo da página", "--bg", false),
				variavel("texto", "Página", "Texto", "--ink", false),
				variavel("textoSuave", "Página", "Texto secundário", "--muted", false),
				variavel("linhas", "Página", "Linhas e bordas", "--line", false),
				variavel("fundoSuave", "Página", "Fundo de campos e destaques", "--soft", false),
				cor("titulos", "Página", "Títulos", "main h1, main h2, main .secao-titulo", "color"),
				cor("links", "Página", "Links do conteúdo",
						"main a:not(.btn-comprar):not(.adm-btn):not(.produto-media):not(.produto-titulo):not(.login-btn)", "color"),

				cor("anunciosFundo", "Barra de avisos", "Fundo", ".barra-anuncios", "background-color"),
				cor("anunciosTexto", "Barra de avisos", "Texto", ".barra-anuncios", "color"),

				cor("topoFundo", "Topo", "Fundo da barra do topo", ".barrabranca", "background-color"),
				cor("topoBotoes", "Topo", "Botões do topo (menu, favoritos, carrinho)",
						".barrabranca .menu-btn, .barrabranca .el-cart-trigger", "color", "border-color"),
				cor("buscaCampoFundo", "Topo", "Campo de busca: fundo", ".search-form input[type=\"text\"]", "background-color"),
				cor("buscaCampoTexto", "Topo", "Campo de busca: texto", ".search-form input[type=\"text\"]", "color"),
				hover("buscaBotaoFundo", "Topo", "Botão buscar: fundo", ".search-form button", "background-color", "border-color"),
				cor("buscaBotaoTexto", "Topo", "Botão buscar: texto", ".search-form button", "color"),
				cor("contadorFundo", "Topo", "Contador do carrinho: fundo", ".el-cart-badge", "background-color"),
				cor("contadorTexto", "Topo", "Contador do carrinho: número", ".el-cart-badge", "color"),

				hover("botaoFundo", "Botões", "Botão principal: fundo",
						".btn-comprar:not(.btn-secundario), .login-btn", "background-color", "border-color"),
				cor("botaoTexto", "Botões", "Botão principal: texto", ".btn-comprar:not(.btn-secundario), .login-btn", "color"),
				hover("botao2Fundo", "Botões", "Botão secundário: fundo",
						".btn-secundario, .adm-btn-sec, .el-cart-clear-btn", "background-color"),
				cor("botao2Texto", "Botões", "Botão secundário: texto", ".btn-secundario, .adm-btn-sec, .el-cart-clear-btn", "color"),
				cor("botao2Borda", "Botões", "Botão secundário: borda", ".btn-secundario, .adm-btn-sec, .el-cart-clear-btn", "border-color"),

				cor("cardFundo", "Produtos", "Fundo do card", ".produto-card", "background-color"),
				cor("cardNome", "Produtos", "Nome do produto", ".produto-card h3 a, .detalhes-titulo", "color"),
				cor("cardPreco", "Produtos", "Preço", ".produto-card .preco, .detalhes-preco", "color"),
				cor("cardParcelamento", "Produtos", "Parcelamento", ".parcelamento, .detalhes-pagamento", "color"),
				cor("favoritoFundo", "Produtos", "Botão de favorito: fundo", ".btn-favorito", "background-color"),
				cor("favoritoIcone", "Produtos", "Botão de favorito: coração", ".btn-favorito", "color"),
				cor("filtroAtivoFundo", "Produtos", "Filtro / página / tamanho selecionado: fundo",
						".adm-chip input:checked + span, .paginacao-item.atual, .detalhes-tamanhos button.ativo",
						"background-color", "border-color"),
				cor("filtroAtivoTexto", "Produtos", "Filtro / página / tamanho selecionado: texto",
						".adm-chip input:checked + span, .paginacao-item.atual, .detalhes-tamanhos button.ativo", "color"),

				cor("menuFundo", "Menu lateral", "Fundo", ".sidebar", "background-color"),
				cor("menuTexto", "Menu lateral", "Links", ".sidebar nav a", "color"),
				cor("menuAdmin", "Menu lateral", "Área do administrador", ".sidebar-admin-titulo, .sidebar nav .sidebar-admin a", "color"),

				cor("carrinhoFundo", "Carrinho", "Fundo", ".el-cart-drawer", "background-color"),
				cor("carrinhoTexto", "Carrinho", "Texto", ".el-cart-drawer", "color"),
				cor("carrinhoRodape", "Carrinho", "Fundo da área do subtotal", ".el-cart-footer", "background-color"),

				cor("caixaFundo", "Caixas e formulários", "Fundo das caixas",
						".cadastro-form-box, .checkout-resumo, .detalhes-compra", "background-color"),
				cor("caixaTexto", "Caixas e formulários", "Texto das caixas",
						".cadastro-form-box, .checkout-resumo, .detalhes-compra", "color"),
				cor("passoFundo", "Caixas e formulários", "Número das etapas da compra: fundo", ".checkout-passo", "background-color"),
				cor("passoTexto", "Caixas e formulários", "Número das etapas da compra: texto", ".checkout-passo", "color"),
				cor("alertaErroFundo", "Caixas e formulários", "Aviso de erro: fundo", ".cadastro-alerta-erro, .login-erro", "background-color"),
				cor("alertaErroTexto", "Caixas e formulários", "Aviso de erro: texto", ".cadastro-alerta-erro, .login-erro", "color"),

				cor("loginPainelFundo", "Login e cadastro", "Painel da marca: fundo", ".login-marca", "background-color"),
				cor("loginPainelTexto", "Login e cadastro", "Painel da marca: texto", ".login-marca, .login-logo, .login-titulo", "color"),
				cor("loginAreaFundo", "Login e cadastro", "Fundo do formulário", ".login-area", "background-color"),

				cor("rodapeFundo", "Rodapé", "Fundo", ".rodape", "background-color"),
				cor("rodapeTexto", "Rodapé", "Texto e links", ".rodape, .rodape-col a, .rodape-base", "color"),
				cor("rodapeTitulos", "Rodapé", "Títulos e nome da loja", ".rodape-col h4, .rodape-logo", "color"),
				cor("rodapeIcones", "Rodapé", "Ícones das redes sociais", ".rodape-redes a", "color", "border-color"));

		PAGINAS = List.of(
				pagina("inicio", "Início (vitrine)", "Página inicial com o catálogo de produtos.", "/home",
						grupo("Página", opc("pg.inicio.titulo", "Nome da página (aba do navegador; vazio = só o nome da loja)", ""),
								t("pg.inicio.tituloSecao", "Título da lista de produtos", "Produtos"))),
				pagina("london", "Vitrine London (/london)", "Vitrine alternativa, sem carrinho.", "/london",
						grupo("Página", opc("pg.london.titulo", "Nome da página (aba do navegador; vazio = só o nome da loja)", ""),
								t("pg.london.tituloSecao", "Título da lista de produtos", "Produtos"))),
				pagina("detalhes", "Detalhes do produto", "Página de cada produto (a aba mostra o nome do produto).", "/detalhes/1",
						grupo("Topo", t("pg.detalhes.voltar", "Link voltar", "Voltar"),
								opc("pg.detalhes.status", "Selo acima do nome", "Novo | +100 vendidos"),
								opc("pg.detalhes.avaliacao", "Avaliação", "4.9 ★★★★★ (120)"),
								opc("pg.detalhes.precoAntigo", "Preço antigo riscado", "R$ 199,90"),
								opc("pg.detalhes.pagamento", "Condição de pagamento", "ou em até 3x sem juros"),
								t("pg.detalhes.rotuloCor", "Rótulo da cor", "Cor:"),
								t("pg.detalhes.rotuloTamanho", "Rótulo do tamanho", "Tamanho:"),
								t("pg.detalhes.ampliar", "Botão ampliar foto", "Ampliar")),
						grupo("Descrição",
								t("pg.detalhes.subtitulo", "Título da descrição", "O que você precisa saber sobre este produto"),
								opc("pg.detalhes.item1", "Destaque 1", "Tecido macio e confortável."),
								opc("pg.detalhes.item2", "Destaque 2", "Modelagem que veste bem no dia a dia."),
								opc("pg.detalhes.item3", "Destaque 3", "Lavagem fácil.")),
						grupo("Caixa de compra",
								opc("pg.detalhes.freteDestaque", "Frete (parte em negrito)", "Frete grátis"),
								opc("pg.detalhes.freteTexto", "Frete (restante)", "a partir de R$ 99 no Sul e de R$ 250 nas demais regiões"),
								opc("pg.detalhes.prazo", "Prazo", "Chegará em poucos dias úteis."),
								t("pg.detalhes.estoque", "Com estoque", "Estoque disponível"),
								t("pg.detalhes.esgotado", "Sem estoque", "Produto esgotado"),
								t("pg.detalhes.quantidade", "Rótulo da quantidade", "Quantidade:"),
								t("pg.detalhes.btnComprar", "Botão comprar agora", "Comprar agora"),
								t("pg.detalhes.btnCarrinho", "Botão adicionar ao carrinho", "Adicionar ao carrinho"),
								t("pg.detalhes.vendidoPor", "Texto antes do nome da loja", "Vendido por"),
								t("pg.detalhes.recomendados", "Título dos recomendados", "Produtos recomendados"))),
				pagina("favoritos", "Lista de desejos", "Produtos marcados com o coração.", "/favoritos",
						grupo("Página", t("pg.favoritos.titulo", "Nome da página (aba do navegador)", "Lista de desejos"),
								t("pg.favoritos.h1", "Título", "Lista de desejos"),
								t("pg.favoritos.vazioTitulo", "Lista vazia: título", "Sua lista de favoritos está vazia"),
								t("pg.favoritos.vazioTexto", "Lista vazia: texto", "Toque no coração dos produtos que você gostar e eles aparecem aqui."),
								t("pg.favoritos.btnVoltar", "Botão voltar", "Voltar para a loja"))),
				pagina("checkout", "Finalizar compra", "Entrega, pagamento e resumo do pedido.", "/checkout",
						grupo("Topo", t("pg.checkout.titulo", "Nome da página (aba do navegador)", "Finalizar compra"),
								t("pg.checkout.voltar", "Link voltar", "Continuar comprando"),
								t("pg.checkout.h1", "Título", "Finalizar compra"),
								t("pg.checkout.etapa1", "Etapa 1", "Carrinho"),
								t("pg.checkout.etapa2", "Etapa 2", "Entrega e pagamento"),
								t("pg.checkout.etapa3", "Etapa 3", "Confirmação")),
						grupo("Carrinho vazio", t("pg.checkout.vazioTitulo", "Título", "Seu carrinho está vazio"),
								t("pg.checkout.vazioTexto", "Texto", "Escolha suas peças na loja e volte aqui para finalizar a compra."),
								t("pg.checkout.vazioBotao", "Botão", "Ir para a loja")),
						grupo("Formulário e resumo", t("pg.checkout.entrega", "Título da entrega", "Entrega"),
								t("pg.checkout.pagamento", "Título do pagamento", "Pagamento"),
								t("pg.checkout.resumo", "Título do resumo", "Resumo"),
								t("pg.checkout.editar", "Botão editar carrinho", "Editar carrinho"),
								t("pg.checkout.btnConfirmar", "Botão confirmar pedido", "Confirmar pedido"),
								opc("pg.checkout.seguro", "Texto abaixo do botão", "Os itens são reservados no estoque assim que você confirma."))),
				pagina("pedido", "Pedido confirmado", "Tela mostrada depois da compra (precisa de um pedido, por isso não tem prévia).", null,
						grupo("Página", t("pg.pedido.titulo", "Nome da página (aba; o número do pedido vem depois)", "Pedido"),
								t("pg.pedido.confirmado", "Título quando o pedido acabou de ser feito", "Pedido confirmado!"),
								t("pg.pedido.codigoRotulo", "Rótulo acima do código do pedido", "Código do pedido"),
								opc("pg.pedido.codigoAviso", "Aviso abaixo do código", "Guarde este código: é com ele que nossa equipe acompanha o seu pedido."),
								t("pg.pedido.itens", "Título dos itens", "Itens"),
								t("pg.pedido.entrega", "Título da entrega", "Entrega"),
								t("pg.pedido.valores", "Título dos valores", "Valores"),
								t("pg.pedido.btnContinuar", "Botão continuar comprando", "Continuar comprando"))),
				pagina("login", "Entrar (login)", "Tela de acesso à conta.", "/login",
						grupo("Página", t("pg.login.titulo", "Nome da página (aba do navegador)", "Entrar"),
								longo("pg.login.painel", "Frase grande do painel (Enter quebra a linha)", "Acesse\nsua conta"),
								t("pg.login.h2", "Título do formulário", "Entrar"),
								t("pg.login.email", "Rótulo do e-mail", "E-mail"),
								t("pg.login.senha", "Rótulo da senha", "Senha"),
								opc("pg.login.esqueci", "Link esqueci a senha", "Esqueci minha senha"),
								t("pg.login.btnEntrar", "Botão entrar", "Entrar"),
								t("pg.login.semConta", "Texto antes do link de cadastro", "Ainda não tem conta?"),
								t("pg.login.linkCriar", "Link criar conta", "Criar conta"),
								t("pg.login.voltar", "Link voltar", "Voltar para a loja"),
								t("pg.login.erro", "Mensagem de senha errada", "E-mail ou senha incorretos."))),
				pagina("criarConta", "Criar conta", "Cadastro de novos clientes.", "/criar-conta",
						grupo("Página", t("pg.criarConta.titulo", "Nome da página (aba do navegador)", "Criar conta"),
								longo("pg.criarConta.painel", "Frase grande do painel (Enter quebra a linha)", "Crie\nsua conta"),
								t("pg.criarConta.h2", "Título do formulário", "Criar conta"),
								t("pg.criarConta.nome", "Rótulo do nome", "Nome"),
								t("pg.criarConta.email", "Rótulo do e-mail", "E-mail"),
								t("pg.criarConta.senha", "Rótulo da senha", "Senha"),
								t("pg.criarConta.confirmar", "Rótulo de confirmar senha", "Confirmar senha"),
								t("pg.criarConta.btnCriar", "Botão criar conta", "Criar conta"),
								t("pg.criarConta.jaTem", "Texto antes do link de login", "Já tem uma conta?"),
								t("pg.criarConta.linkLogin", "Link fazer login", "Fazer login"),
								t("pg.criarConta.voltar", "Link voltar", "Voltar para a loja"))),
				pagina("perfil", "Meu perfil", "Dados da conta do cliente.", "/perfil",
						grupo("Página", t("pg.perfil.titulo", "Nome da página (aba do navegador)", "Meu perfil"),
								t("pg.perfil.h1", "Título", "Meu perfil"),
								opc("pg.perfil.sub", "Subtítulo", "Veja os dados da sua conta e altere seu e-mail ou sua senha."),
								t("pg.perfil.dadosConta", "Título: dados da conta", "Dados da conta"),
								t("pg.perfil.dadosPessoais", "Título: dados pessoais", "Dados pessoais"),
								t("pg.perfil.btnDados", "Botão salvar dados pessoais", "Salvar dados pessoais"),
								t("pg.perfil.alterarEmail", "Título: alterar e-mail", "Alterar e-mail"),
								t("pg.perfil.btnEmail", "Botão salvar e-mail", "Salvar e-mail"),
								t("pg.perfil.alterarSenha", "Título: alterar senha", "Alterar senha"),
								t("pg.perfil.btnSenha", "Botão salvar senha", "Salvar senha"))),
				institucional("sobre", "Sobre nós", "/sobre", "Estilo London Store", "Sobre nós",
						"Somos uma loja de moda com estilo urbano, feita para quem quer se vestir bem no dia a dia sem complicação.",
						t("pg.sobre.btn", "Botão no fim da página", "Conhecer a coleção")),
				institucional("frete", "Frete e Entrega", "/frete-e-entrega", "Atendimento", "Frete e Entrega",
						"Enviamos para todo o Brasil. O valor do frete aparece no resumo assim que você escolhe o estado de entrega, antes de confirmar o pedido."),
				institucional("trocas", "Trocas e Devoluções", "/trocas-e-devolucoes", "Atendimento", "Trocas e Devoluções",
						"Não serviu ou não gostou? Você tem até 30 dias, a partir do recebimento, para trocar a peça."),
				institucional("privacidade", "Política de Privacidade", "/politica-de-privacidade", "Seus dados", "Política de Privacidade",
						"Explicamos aqui quais dados coletamos quando você usa a loja, para que usamos e quais são os seus direitos."),
				pagina("adminPedidos", "Admin: pedidos", "Lista de vendas com o código de cada pedido.", "/admin/pedidos",
						grupo("Página", t("pg.adminPedidos.titulo", "Nome da página (aba do navegador)", "Pedidos"),
								t("pg.adminPedidos.h1", "Título", "Pedidos"),
								t("pg.adminPedidos.buscaPlaceholder", "Texto dentro da busca", "Código, cliente ou e-mail"),
								t("pg.adminPedidos.btnBuscar", "Botão da busca", "Abrir pedido"))),
				pagina("adminPedido", "Admin: acompanhar pedido", "Detalhes e etapas de um pedido (precisa de um pedido, por isso não tem prévia).", null,
						grupo("Página", t("pg.adminPedido.titulo", "Nome da página (aba; o código vem depois)", "Pedido"),
								t("pg.adminPedido.voltar", "Link voltar", "Voltar para pedidos"),
								t("pg.adminPedido.historico", "Título do histórico", "Histórico"),
								t("pg.adminPedido.atualizar", "Título do acompanhamento", "Acompanhamento"),
								t("pg.adminPedido.btnSalvar", "Botão salvar", "Salvar acompanhamento"))),
				pagina("adminProdutos", "Admin: lista de produtos", "Painel com todos os produtos.", "/admin/produto/listar",
						grupo("Página", t("pg.adminProdutos.titulo", "Nome da página (aba do navegador)", "Listagem de produtos"),
								t("pg.adminProdutos.h1", "Título", "Produtos"),
								t("pg.adminProdutos.btnNovo", "Botão novo produto", "+ Novo produto"))),
				pagina("adminCadastrar", "Admin: novo produto", "Cadastro de produto.", "/admin/produto/cadastrar",
						grupo("Página", t("pg.adminCadastrar.titulo", "Nome da página (aba do navegador)", "Cadastro de Produtos"),
								t("pg.adminCadastrar.voltar", "Link voltar", "Voltar para produtos"),
								t("pg.adminCadastrar.h2", "Título do formulário", "Novo produto"),
								t("pg.adminCadastrar.btnCancelar", "Botão cancelar", "Cancelar"),
								t("pg.adminCadastrar.btnSalvar", "Botão salvar", "Salvar produto"),
								t("pg.adminCadastrar.lista", "Título da lista abaixo", "Produtos cadastrados"))),
				pagina("adminEditar", "Admin: editar produto", "Edição de um produto.", "/admin/produto/editar/1",
						grupo("Página", t("pg.adminEditar.titulo", "Nome da página (aba do navegador)", "Editar produto"),
								t("pg.adminEditar.voltar", "Link voltar", "Voltar para produtos"),
								t("pg.adminEditar.h2", "Título do formulário", "Editar produto"),
								t("pg.adminEditar.btnCancelar", "Botão cancelar", "Cancelar"),
								t("pg.adminEditar.btnSalvar", "Botão salvar", "Salvar alterações"))),
				pagina("adminVisualizar", "Admin: ver produto", "Ficha e métricas de um produto.", "/admin/produto/visualizar/1",
						grupo("Página", t("pg.adminVisualizar.titulo", "Nome da página (aba do navegador)", "Visualizar produto"),
								t("pg.adminVisualizar.voltar", "Link voltar", "Voltar para produtos"),
								t("pg.adminVisualizar.btnLoja", "Botão ver na loja", "Ver na loja"),
								t("pg.adminVisualizar.btnEditar", "Botão editar", "Editar produto"))),
				pagina("adminConfig", "Admin: configurações", "Esta tela.", null,
						grupo("Página", t("pg.adminConfig.titulo", "Nome da página (aba do navegador)", "Configurações"),
								t("pg.adminConfig.h1", "Título", "Configurações"))));

		List<Secao> secoes = new ArrayList<>();
		secoes.add(new Secao("aparencia", "Cor principal e logo", "aparencia", "Identidade visual do site todo.", null));
		secoes.add(new Secao("textos", "Textos do site todo", "textos",
				"Nome da loja, avisos, menu, carrinho, filtros e rodapé: aparecem em todas as páginas.", null));
		secoes.add(new Secao("cores", "Cores de cada item", "cores",
				"Cor de cada parte da interface em todas as páginas. Cada página pode trocar de novo na própria seção.", null));
		for (Pagina p : PAGINAS) {
			secoes.add(new Secao("pg-" + p.id(), p.nome(), "pagina", p.descricao(), p));
		}
		SECOES = Collections.unmodifiableList(secoes);

		TEXTOS_GERAIS.forEach(g -> g.campos().forEach(c -> CAMPOS.put(c.chave(), c)));
		PAGINAS.forEach(p -> {
			PAGINAS_POR_ID.put(p.id(), p);
			p.grupos().forEach(g -> g.campos().forEach(c -> CAMPOS.put(c.chave(), c)));
		});
		CORES.forEach(c -> CORES_POR_ID.put(c.id(), c));
		SECOES.forEach(s -> SECOES_POR_ID.put(s.id(), s));
	}

	private CatalogoPersonalizacao() {
	}

	// ---------- consultas ----------

	public static List<Grupo> textosGerais() {
		return TEXTOS_GERAIS;
	}

	public static List<Pagina> paginas() {
		return PAGINAS;
	}

	public static Pagina pagina(String id) {
		return PAGINAS_POR_ID.get(id);
	}

	public static List<Secao> secoes() {
		return SECOES;
	}

	public static Secao secao(String id) {
		return id == null ? null : SECOES_POR_ID.get(id);
	}

	public static Map<String, Campo> campos() {
		return Collections.unmodifiableMap(CAMPOS);
	}

	public static Campo campo(String chave) {
		return CAMPOS.get(chave);
	}

	public static ItemCor itemCor(String id) {
		return CORES_POR_ID.get(id);
	}

	/** Grupos de cores para o site todo (sem os que só fazem sentido numa página) ou para uma página. */
	public static List<GrupoCores> gruposDeCores(boolean paraPagina) {
		Map<String, List<ItemCor>> porGrupo = new LinkedHashMap<>();
		for (ItemCor c : CORES) {
			if (c.somentePagina() && !paraPagina) {
				continue; // a cor principal do site todo fica na seção "Cor principal e logo"
			}
			porGrupo.computeIfAbsent(c.grupo(), k -> new ArrayList<>()).add(c);
		}
		List<GrupoCores> grupos = new ArrayList<>();
		porGrupo.forEach((nome, itens) -> grupos.add(new GrupoCores(nome, List.copyOf(itens))));
		return grupos;
	}

	public static String chaveCor(String paginaId, String itemId) {
		return paginaId == null ? "cor." + itemId : "pg." + paginaId + ".cor." + itemId;
	}

	/** Todas as chaves que pertencem à seção (as únicas que o formulário dela pode gravar). */
	public static List<String> chavesDaSecao(Secao secao) {
		List<String> chaves = new ArrayList<>();
		switch (secao.tipo()) {
			case "textos" -> TEXTOS_GERAIS.forEach(g -> g.campos().forEach(c -> chaves.add(c.chave())));
			case "cores" -> gruposDeCores(false).forEach(g -> g.itens().forEach(c -> chaves.add(chaveCor(null, c.id()))));
			case "pagina" -> {
				secao.pagina().grupos().forEach(g -> g.campos().forEach(c -> chaves.add(c.chave())));
				gruposDeCores(true).forEach(g -> g.itens().forEach(c -> chaves.add(chaveCor(secao.pagina().id(), c.id()))));
			}
			default -> {
			}
		}
		return chaves;
	}

	/** Item de cor de uma chave cor.* ou pg.*.cor.* (null se a chave não for de cor). */
	public static ItemCor itemDaChave(String chave) {
		int i = chave.lastIndexOf(".cor.");
		if (chave.startsWith("cor.")) {
			return CORES_POR_ID.get(chave.substring(4));
		}
		return i < 0 ? null : CORES_POR_ID.get(chave.substring(i + 5));
	}

	/** Página dona de uma chave pg.* (null para as do site todo). */
	public static Pagina paginaDaChave(String chave) {
		if (!chave.startsWith("pg.")) {
			return null;
		}
		int fim = chave.indexOf('.', 3);
		return fim < 0 ? null : PAGINAS_POR_ID.get(chave.substring(3, fim));
	}

	// ---------- construção ----------

	private static Campo t(String chave, String rotulo, String padrao) {
		return new Campo(chave, rotulo, padrao, false, false);
	}

	private static Campo opc(String chave, String rotulo, String padrao) {
		return new Campo(chave, rotulo, padrao, false, true);
	}

	private static Campo longo(String chave, String rotulo, String padrao) {
		return new Campo(chave, rotulo, padrao, true, false);
	}

	private static Grupo grupo(String nome, Campo... campos) {
		return new Grupo(nome, List.of(campos));
	}

	private static Pagina pagina(String id, String nome, String descricao, String url, Grupo... grupos) {
		return new Pagina(id, nome, descricao, url, List.of(grupos));
	}

	private static Pagina institucional(String id, String nome, String url, String kicker, String h1, String lead, Campo... extras) {
		List<Campo> campos = new ArrayList<>(List.of(
				t("pg." + id + ".titulo", "Nome da página (aba do navegador)", nome),
				opc("pg." + id + ".kicker", "Texto pequeno acima do título", kicker),
				t("pg." + id + ".h1", "Título", h1),
				longo("pg." + id + ".lead", "Texto de abertura", lead)));
		campos.addAll(Arrays.asList(extras));
		return new Pagina(id, nome, "Página institucional do rodapé.", url, List.of(new Grupo("Página", List.copyOf(campos))));
	}

	private static ItemCor cor(String id, String grupo, String rotulo, String seletor, String... propriedades) {
		return new ItemCor(id, grupo, rotulo, seletor, List.of(propriedades), null, false, false);
	}

	private static ItemCor hover(String id, String grupo, String rotulo, String seletor, String... propriedades) {
		return new ItemCor(id, grupo, rotulo, seletor, List.of(propriedades), null, true, false);
	}

	private static ItemCor variavel(String id, String grupo, String rotulo, String variavel, boolean somentePagina) {
		return new ItemCor(id, grupo, rotulo, null, List.of(), variavel, false, somentePagina);
	}
}
