# Radar Financeiro

App Android para rastreamento automatico de gastos no cartao de credito e PIX.

## Como instalar

### Passo 1: Subir o projeto no GitHub
1. Acesse github.com e faca login
2. Clique em "+" no canto superior direito e depois "New repository"
3. Nome: `radar-financeiro`
4. Marque "Public"
5. Clique "Create repository"
6. Na pagina do repositorio, clique em "uploading an existing file"
7. Arraste TODOS os arquivos desta pasta para a area de upload
8. Clique "Commit changes"

### Passo 2: Baixar o APK
1. No repositorio, clique na aba "Actions"
2. Aguarde o build terminar (leva ~5 minutos)
3. Clique no build concluido
4. Em "Artifacts", clique em "RadarFinanceiro-debug" para baixar o APK

### Passo 3: Instalar no celular
1. Transfira o APK para o celular (email, WhatsApp, Google Drive)
2. Abra o APK no celular
3. Permita "Instalar de fontes desconhecidas" se solicitado
4. Instale e abra o app
5. Na tela inicial, ative as permissoes solicitadas

## Permissoes necessarias
- **Acesso a notificacoes**: captura compras do Itau e Carteira do Google
- **SMS**: captura compras do Bradesco/Amazon
- **Localizacao**: registra onde voce estava ao comprar
- **Sobrepor outros apps**: mostra o pop-up de anotacao

## Funcionalidades
- Captura automatica de compras via notificacao (Itau) e SMS (Bradesco)
- Pop-up para anotar o que voce comprou
- GPS automatico no momento da compra
- Separacao de gastos (Gustavo vs Luciana)
- Aprendizado: memoriza classificacoes para sugerir automaticamente
- Resumo mensal por categoria
- Busca por estabelecimento ou nota
- Exportacao CSV
- Backup automatico local (Google Drive em versao futura)
