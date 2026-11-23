# Checkout: endereço salvo e CPF para Pix

- O checkout carrega os dados de entrega salvos no cadastro do usuário.
- Ao finalizar, os dados de entrega, o serviço de frete escolhido e o CPF são persistidos no usuário.
- O pedido continua mantendo uma cópia própria dos dados de entrega e do frete aplicado, preservando o histórico mesmo que o cliente altere o endereço depois.
- O CPF é validado pelos dígitos verificadores antes de finalizar; ele é necessário porque o Asaas respondeu `invalid_object` informando que a cobrança exige CPF/CNPJ do cliente.
- Se o cadastro de cliente já existir no Asaas sem CPF/CNPJ, o cliente é atualizado antes de criar a cobrança.
- A consulta de CEP usa ViaCEP no navegador; se indisponível, os campos podem ser preenchidos manualmente.

O Hibernate `ddl-auto=update` adicionará as novas colunas da entidade `Usuario` ao banco existente. Não apague a pasta `dados/`.
