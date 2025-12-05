import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.layout.element.Image;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Main {
    public static void main(String[] args) throws Exception {
        // Detecta se está rodando sem bibliotecas (Run Java do VS Code)
        try {
            Class.forName("com.itextpdf.kernel.pdf.PdfDocument");
        } catch (ClassNotFoundException e) {
            System.out.println("\n=== EXECUTANDO VIA JAR COM BIBLIOTECAS ===\n");
            String jarPath = "target\\cearaprev-report-jar-with-dependencies.jar";
            
            // Tenta encontrar o JAR na raiz do projeto
            String dirAtual = System.getProperty("user.dir");
            File jarFile = new File(dirAtual, jarPath);
            
            if (!jarFile.exists() && dirAtual.endsWith("src\\main\\java")) {
                File dirProjeto = new File(dirAtual).getParentFile().getParentFile().getParentFile();
                jarFile = new File(dirProjeto, jarPath);
            }
            
            if (jarFile.exists()) {
                // Executa o JAR com as bibliotecas
                ProcessBuilder pb1 = new ProcessBuilder("java", "-jar", jarFile.getAbsolutePath());
                pb1.inheritIO();
                Process p1 = pb1.start();
                p1.waitFor();
                
                // Executa também o modo inativos
                System.out.println("\n\n=== GERANDO RELATÓRIO DE INATIVOS ===\n");
                ProcessBuilder pb2 = new ProcessBuilder("java", "-jar", jarFile.getAbsolutePath(), "inativos");
                pb2.inheritIO();
                Process p2 = pb2.start();
                p2.waitFor();
                
                System.out.println("\n✅ AMBOS OS RELATÓRIOS FORAM GERADOS COM SUCESSO!");
            } else {
                System.out.println("ERRO: JAR não encontrado em: " + jarFile.getAbsolutePath());
                System.out.println("Execute: mvn clean package");
            }
            return;
        }
        
        System.setProperty("java.awt.headless", "true");

        // Verifica se deve filtrar apenas inativos
        boolean apenasInativos = args.length > 0 && args[0].equalsIgnoreCase("inativos");
        
        if (apenasInativos) {
            System.out.println("\n>>> MODO: RELATÓRIO APENAS DE INATIVOS <<<\n");
        } else {
            System.out.println("\n>>> MODO: RELATÓRIO GERAL (TODOS) <<<");
            System.out.println("    Para gerar relatório apenas de inativos, execute: java -jar programa.jar inativos\n");
        }

        // Determinar o diretório do projeto (3 níveis acima de src/main/java)
        String dirAtual = System.getProperty("user.dir");
        File csvFile = new File(dirAtual, "todos_os_lotes.csv");
        
        // Se não encontrar no diretório atual, tentar na raiz do projeto
        if (!csvFile.exists()) {
            File dirProjeto = new File(dirAtual).getParentFile().getParentFile().getParentFile();
            if (dirProjeto != null) {
                csvFile = new File(dirProjeto, "todos_os_lotes.csv");
            }
        }

        if (!csvFile.exists()) {
            System.out.println("ERRO: Coloque o arquivo 'todos_os_lotes.csv' na raiz do projeto!");
            System.out.println("Caminho esperado: " + csvFile.getAbsolutePath());
            return;
        }

        // Totais oficiais por lote (para relatório geral)
        long[] totalOficial = new long[]{0, 8611, 9202, 10377, 10622};
        // Totais oficiais de inativos (fornecidos manualmente)
        long[] totalOficialInativos = new long[]{0, 2765, 5348, 5552, 4417};
        
        long[] desbloqueados = new long[5];
        double[] valorRetido = new double[5];
        double[] valorBloqueados = new double[5];
        long[] totalInativosPorLote = new long[5]; // Total de inativos contados do CSV

        try (BufferedReader br = Files.newBufferedReader(csvFile.toPath(), StandardCharsets.UTF_8);
             CSVParser parser = new CSVParser(br, CSVFormat.DEFAULT.withFirstRecordAsHeader().withDelimiter(';').withIgnoreEmptyLines(true).withTrim(true).withAllowMissingColumnNames(true))) {

            for (CSVRecord r : parser) {
                String loteStr = r.get("lote_suspensao").trim();
                if (loteStr.isEmpty()) continue;
                int lote;
                try {
                    lote = Integer.parseInt(loteStr);
                } catch (NumberFormatException ex) {
                    continue;
                }
                if (lote < 1 || lote >= desbloqueados.length) continue;

                String valorCampo = r.get("valor").trim();
                // transforma 1.234,56 em 1234.56
                String valorNormalized = valorCampo.replace(".", "").replace(",", ".");
                double valor = 0.0;
                if (!valorNormalized.isEmpty()) {
                    try {
                        valor = Double.parseDouble(valorNormalized);
                    } catch (NumberFormatException ignored) {
                    }
                }

                // Lidar com BOM no primeiro cabeçalho
                String statusCearaprev = "";
                String statusRecadastro = "";
                String vinculo = "";
                
                try {
                    statusCearaprev = r.get("status_cearaprev");
                } catch (IllegalArgumentException e) {
                    statusCearaprev = r.get("\uFEFFstatus_cearaprev"); // Com BOM
                }
                
                try {
                    statusRecadastro = r.get("status_recadastro");
                } catch (IllegalArgumentException e) {
                    statusRecadastro = r.get("\uFEFFstatus_recadastro"); // Com BOM
                }
                
                try {
                    vinculo = r.get("status_vinculo");
                } catch (IllegalArgumentException e) {
                    vinculo = r.get("\uFEFFstatus_vinculo"); // Com BOM
                }
                
                boolean provaFeita = statusCearaprev.contains("REALIZADA");
                boolean recadastroFeito = statusRecadastro.contains("REALIZADO");
                
                // Verifica se é inativo (APOSENTADO, PENSIONISTA, PENSIONISTA_NAO_PREVIDENCIARIO)
                boolean isInativo = !vinculo.contains("ATIVO") && !vinculo.contains("AGUARDANDO");
                
                // Contabiliza total de inativos por lote (sempre, independente do modo)
                if (isInativo) {
                    totalInativosPorLote[lote] += 1;
                }
                
                // Se modo inativos, pula registros que não são inativos
                if (apenasInativos && !isInativo) {
                    continue;
                }

                boolean desbloq;
                if (provaFeita) {
                    if (vinculo.contains("ATIVO") || vinculo.contains("AGUARDANDO")) {
                        desbloq = recadastroFeito;
                    } else {
                        desbloq = true;
                    }
                } else {
                    desbloq = false;
                }

                if (desbloq) {
                    desbloqueados[lote] += 1;
                }
                valorRetido[lote] += valor;
            }
        }

        NumberFormat nf = NumberFormat.getInstance(new Locale("pt", "BR"));
        nf.setMaximumFractionDigits(2);
        
        // Formata data e hora
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        String dataHora = sdf.format(new Date());

        System.out.println("\n");
        System.out.println("       CEARAPREV  RELATÓRIO FINAL" + (apenasInativos ? " - INATIVOS" : ""));
        System.out.println("       Data/Hora: " + dataHora);
        System.out.println("");
        System.out.printf("%-8s %-16s %-16s %-16s %s%n",
                "REMESSA", "TOTAL DA REMESSA", "DESBLOQUEADOS", "EXCLUSÃO", "RECURSOS RETIDOS");

        long totalDesbloq = 0;
        double totalValor = 0;
        double totalValorBloqueados = 0;
        long totalGeralProcessado = 0;

        for (int i = 1; i <= 4; i++) {
            // Usa os totais oficiais de inativos quando no modo inativos
            long totalUsado = apenasInativos ? totalOficialInativos[i] : totalOficial[i];
            long bloqueado = totalUsado - desbloqueados[i];
            
            // Calcula valor médio por pessoa e multiplica pela quantidade de excluídos
            // Para inativos, usa o total do CSV para calcular valor médio
            long totalParaMedia = apenasInativos ? totalInativosPorLote[i] : totalUsado;
            double valorMedio = totalParaMedia > 0 ? valorRetido[i] / totalParaMedia : 0;
            valorBloqueados[i] = valorMedio * bloqueado;
            
            totalDesbloq += desbloqueados[i];
            totalValor += valorRetido[i];
            totalValorBloqueados += valorBloqueados[i];
            totalGeralProcessado += totalUsado;

            System.out.printf("%s      %16s %16s %16s  R$ %18s%n",
                    i + "ª",
                    nf.format(totalUsado),
                    nf.format(bloqueado),
                    nf.format(desbloqueados[i]),
                    nf.format(valorBloqueados[i]));
        }

        System.out.println("");
        System.out.printf("TOTAL   %16s %16s %16s  R$ %18s%n",
                nf.format(totalGeralProcessado),
                nf.format(totalGeralProcessado - totalDesbloq),
                nf.format(totalDesbloq),
                nf.format(totalValorBloqueados));
        System.out.println("\n");
        
        String sufixo = apenasInativos ? "_inativos" : "";
        System.out.println("Gráfico salvo na pasta do projeto:");
        System.out.println("   grafico_economia" + sufixo + ".png");

        // Gera gráfico único de economia dos 4 lotes
        gerarGraficoEconomia4Lotes(valorBloqueados, nf, apenasInativos);

        // Gera relatório em PDF
        gerarRelatorioPDF(apenasInativos ? totalOficialInativos : totalOficial, desbloqueados, valorRetido, valorBloqueados, 
                         totalDesbloq, totalValor, totalValorBloqueados, totalGeralProcessado, nf, apenasInativos, dataHora);
    }

    private static String getDiretorioSaida() {
        String dirAtual = System.getProperty("user.dir");
        File dir = new File(dirAtual);
        // Se estiver em src/main/java, subir para raiz do projeto
        if (dirAtual.endsWith("src\\main\\java") || dirAtual.endsWith("src/main/java")) {
            dir = dir.getParentFile().getParentFile().getParentFile();
        }
        return dir.getAbsolutePath();
    }

    private static void gerarGraficoEconomia4Lotes(double[] valorBloqueados, NumberFormat nf, boolean apenasInativos) throws IOException {
        DefaultPieDataset data = new DefaultPieDataset();
        data.setValue("Lote 1ª: R$ " + nf.format(valorBloqueados[1]), valorBloqueados[1]);
        data.setValue("Lote 2ª: R$ " + nf.format(valorBloqueados[2]), valorBloqueados[2]);
        data.setValue("Lote 3ª: R$ " + nf.format(valorBloqueados[3]), valorBloqueados[3]);
        data.setValue("Lote 4ª: R$ " + nf.format(valorBloqueados[4]), valorBloqueados[4]);

        String titulo = apenasInativos ? "ECONOMIA DO ESTADO POR LOTE - INATIVOS" : "ECONOMIA DO ESTADO POR LOTE - EXCLUSÃO";
        JFreeChart chart = ChartFactory.createRingChart(
                titulo,
                data, false, true, false); // Criar SEM legenda

        // Aumentar fonte do título para 18
        chart.getTitle().setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 18));

        // Definir cores personalizadas RGB mais vibrantes: Verde, Laranja/Amarelo, Azul, Vermelho
        org.jfree.chart.plot.RingPlot plot = (org.jfree.chart.plot.RingPlot) chart.getPlot();
        java.awt.Color corVerde = new java.awt.Color(102, 255, 102);
        java.awt.Color corLaranja = new java.awt.Color(255, 187, 51);
        java.awt.Color corAzul = new java.awt.Color(77, 171, 247);
        java.awt.Color corVermelha = new java.awt.Color(255, 102, 102);
        plot.setSectionPaint("Lote 1ª: R$ " + nf.format(valorBloqueados[1]), corVerde); // Verde vibrante
        plot.setSectionPaint("Lote 2ª: R$ " + nf.format(valorBloqueados[2]), corLaranja); // Laranja/Amarelo vibrante
        plot.setSectionPaint("Lote 3ª: R$ " + nf.format(valorBloqueados[3]), corAzul); // Azul vibrante
        plot.setSectionPaint("Lote 4ª: R$ " + nf.format(valorBloqueados[4]), corVermelha); // Vermelho vibrante
        
        // Criar legenda customizada usando PaintList para símbolos coloridos
        org.jfree.chart.title.LegendTitle legenda = new org.jfree.chart.title.LegendTitle(plot);
        legenda.setItemFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 18));
        legenda.setPosition(org.jfree.chart.ui.RectangleEdge.BOTTOM);
        
        // Configurar arranjo em 2 linhas x 2 colunas
        org.jfree.chart.block.BlockContainer wrapper = legenda.getItemContainer();
        wrapper.setArrangement(new org.jfree.chart.block.GridArrangement(2, 2));
        
        chart.addSubtitle(legenda);
        
        // Centro maior para rosca mais grossa - 0.10 * 6 = 0.60
        plot.setSectionDepth(0.60);
        
        // Visual mais limpo - sem separadores
        plot.setSeparatorsVisible(false);
        
        // Sem borda externa para visual mais limpo
        plot.setOutlineVisible(false);
        
        // Labels mais limpos
        plot.setLabelGenerator(null); // Remove labels nas fatias
        plot.setSimpleLabels(true);
        
        // Fundo branco limpo
        chart.setBackgroundPaint(java.awt.Color.WHITE);
        plot.setBackgroundPaint(java.awt.Color.WHITE);
        
        // Remover sombras para visual mais limpo
        plot.setShadowPaint(null);

        String dirSaida = getDiretorioSaida();
        String sufixo = apenasInativos ? "_inativos" : "";
        File out = new File(dirSaida + File.separator + "grafico_economia" + sufixo + ".png");
        ChartUtils.saveChartAsPNG(out, chart, 800, 600);
    }

    private static void gerarRelatorioPDF(long[] totalOficial, long[] desbloqueados, double[] valorRetido, double[] valorBloqueados,
                                         long totalDesbloq, double totalValor, double totalValorBloqueados, long totalGeral, 
                                         NumberFormat nf, boolean apenasInativos, String dataHora) throws IOException {
        String dirSaida = getDiretorioSaida();
        String sufixo = apenasInativos ? "_inativos" : "";
        String filename = dirSaida + File.separator + "relatorio_cearaprev" + sufixo + ".pdf";
        new File(filename).delete(); // Remove arquivo antigo
        
        PdfWriter writer = new PdfWriter(filename);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document doc = new Document(pdfDoc);
        doc.setMargins(20, 20, 20, 20);

        // CABEÇALHO PROFISSIONAL
        Table headerTable = new Table(1);
        headerTable.setWidth(UnitValue.createPercentValue(100));
        String tituloRelatorio = apenasInativos ? "RELATÓRIO DE DESBLOQUEIOS - INATIVOS" : "RELATÓRIO DE DESBLOQUEIOS";
        Cell headerMainCell = new Cell()
                .add(new Paragraph("CEARAPREV")
                        .setFontSize(20)
                        .setBold()
                        .setFontColor(ColorConstants.WHITE))
                .add(new Paragraph(tituloRelatorio)
                        .setFontSize(11)
                        .setFontColor(ColorConstants.WHITE))
                .setBackgroundColor(new DeviceRgb(0, 102, 204))
                .setPadding(15)
                .setBorder(null);
        headerTable.addCell(headerMainCell);
        doc.add(headerTable);
        
        // Data e hora do relatório
        doc.add(new Paragraph("Data/Hora: " + dataHora)
                .setFontSize(9)
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.RIGHT)
                .setMarginTop(5)
                .setMarginBottom(5));
        
        doc.add(new Paragraph("\n").setFontSize(6));

        // TABELA PRINCIPAL
        Table table = new Table(new float[]{1, 2.5f, 2, 2, 3});
        table.setWidth(UnitValue.createPercentValue(100));
        
        // Cabeçalho (azul escuro)
        String[] headers = {"LOTE", "TOTAL", "DESBLOQUEADOS", "EXCLUSÃO", "VALOR RETIDO"};
        for (String header : headers) {
            Cell headerCell = new Cell()
                    .add(new Paragraph(header)
                            .setBold()
                            .setFontSize(9)
                            .setFontColor(ColorConstants.WHITE))
                    .setBackgroundColor(new DeviceRgb(0, 71, 171))
                    .setPadding(8)
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER);
            table.addCell(headerCell);
        }

        // Dados dos lotes com cores alternadas
        for (int i = 1; i <= 4; i++) {
            long bloqueado = totalOficial[i] - desbloqueados[i];
            DeviceRgb bgColor = (i % 2 == 0) ? new DeviceRgb(240, 248, 255) : new DeviceRgb(255, 255, 255);
            
            Cell cell1 = new Cell().add(new Paragraph(i + "ª").setFontSize(9).setBold()).setPadding(6).setBackgroundColor(bgColor);
            Cell cell2 = new Cell().add(new Paragraph(nf.format(totalOficial[i])).setFontSize(8)).setPadding(6).setBackgroundColor(bgColor);
            Cell cell3 = new Cell().add(new Paragraph(nf.format(bloqueado)).setFontSize(8)).setPadding(6).setBackgroundColor(bgColor);
            Cell cell4 = new Cell().add(new Paragraph(nf.format(desbloqueados[i]) + " (" + String.format("%.1f%%", (desbloqueados[i]*100.0/totalOficial[i])).replace('.',',') + ")").setFontSize(8)).setPadding(6).setBackgroundColor(bgColor);
            Cell cell5 = new Cell().add(new Paragraph("R$ " + nf.format(valorBloqueados[i])).setFontSize(8)).setPadding(6).setBackgroundColor(bgColor);
            
            table.addCell(cell1);
            table.addCell(cell2);
            table.addCell(cell3);
            table.addCell(cell4);
            table.addCell(cell5);
        }

        // Linha de TOTAL (verde)
        long totalBloqueado = totalGeral - totalDesbloq;
        DeviceRgb totalBgColor = new DeviceRgb(0, 150, 88);
        
        Cell totalCell1 = new Cell().add(new Paragraph("TOTAL").setBold().setFontSize(9).setFontColor(ColorConstants.WHITE)).setPadding(8).setBackgroundColor(totalBgColor);
        Cell totalCell2 = new Cell().add(new Paragraph(nf.format(totalGeral)).setBold().setFontSize(9).setFontColor(ColorConstants.WHITE)).setPadding(8).setBackgroundColor(totalBgColor);
        Cell totalCell3 = new Cell().add(new Paragraph(nf.format(totalBloqueado)).setBold().setFontSize(9).setFontColor(ColorConstants.WHITE)).setPadding(8).setBackgroundColor(totalBgColor);
        Cell totalCell4 = new Cell().add(new Paragraph(nf.format(totalDesbloq) + " (" + String.format("%.1f%%", (totalDesbloq*100.0/totalGeral)).replace('.',',') + ")").setBold().setFontSize(9).setFontColor(ColorConstants.WHITE)).setPadding(8).setBackgroundColor(totalBgColor);
        Cell totalCell5 = new Cell().add(new Paragraph("R$ " + nf.format(totalValorBloqueados)).setBold().setFontSize(9).setFontColor(ColorConstants.WHITE)).setPadding(8).setBackgroundColor(totalBgColor);
        
        table.addCell(totalCell1);
        table.addCell(totalCell2);
        table.addCell(totalCell3);
        table.addCell(totalCell4);
        table.addCell(totalCell5);

        doc.add(table);
        doc.add(new Paragraph("\n\n").setFontSize(8));

        // Seção de gráfico único - ECONOMIA DO ESTADO
        Paragraph graficosTitle = new Paragraph("ANÁLISE VISUAL - ECONOMIA DO ESTADO POR LOTE")
                .setFontSize(13)
                .setBold()
                .setMarginTop(10)
                .setMarginBottom(10)
                .setFontColor(new DeviceRgb(0, 102, 204))
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER);
        doc.add(graficosTitle);

        String nomeGrafico = dirSaida + File.separator + "grafico_economia" + sufixo + ".png";
        File imgFile = new File(nomeGrafico);
        if (imgFile.exists()) {
            try {
                Image img = new Image(ImageDataFactory.create(nomeGrafico));
                img.scaleToFit(450, 320);
                img.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
                doc.add(img);
            } catch (Exception e) {
                doc.add(new Paragraph("Erro ao carregar gráfico de economia."));
            }
        }

        doc.close();
        
        System.out.println("Relatório PDF gerado: " + filename);
    }
}
