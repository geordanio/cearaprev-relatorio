import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.Locale;

public class Main {
    public static void main(String[] args) throws Exception {
        File csvFile = new File("todos_os_lotes.csv");

        if (!csvFile.exists()) {
            System.out.println("ERRO: Coloque o arquivo 'todos_os_lotes.csv' na mesma pasta do programa!");
            System.out.println("Baixe ou arraste sua planilha para cá e rode novamente.");
            return;
        }

        long[] totalOficial = {0, 8611,  , 9202  , 10377 , 10622};
        long[] desbloqueados = new long[5];
        double[] valorRetido = new double[5];

        try (CSVParser parser = new CSVParser(new FileReader(csvFile, StandardCharsets.UTF_8),
                CSVFormat.DEFAULT.withFirstRecordAsHeader().withDelimiter(';'))) {

            for (CSVRecord r : parser) {
                int lote = Integer.parseInt(r.get("lote_suspensao").trim());
                double valor = Double.parseDouble(r.get("valor").replace(",", "."));

                boolean provaFeita = r.get("status_cearaprev").contains("REALIZADA");
                boolean recadastroFeito = r.get("status_recadastro").contains("REALIZADO");
                String vinculo = r.get("status_vinculo");

                boolean desbloq = provaFeita && (
                    vinculo.contains("ATIVO") || vinculo.contains("AGUARDANDO")
                        ? recadastroFeito
                        : true
                );

                desbloqueados[lote] += desbloq ? 1 : 0;
                valorRetido[lote] += valor;
            }
        }

        NumberFormat nf = NumberFormat.getInstance(new Locale("pt", "BR"));
        nf.setMaximumFractionDigits(2);

        System.out.println("\n════════════════════════════════════");
        System.out.println("       CEARAPREV – RELATÓRIO FINAL");
        System.out.println("════════════════════════════════════");
        System.out.printf("%-8s %-16s %-16s %-16s %s%n",
                "REMESSA", "TOTAL DA REMESSA", "DESBLOQUEADOS", "EXCLUSÃO", "RECURSOS RETIDOS");

        long totalDesbloq = 0;
        double totalValor = 0;

        for (int i = 1; i <= 4; i++) {
            long exclusao = totalOficial[i] - desbloqueados[i];
            totalDesbloq += desbloqueados[i];
            totalValor += valorRetido[i];

            System.out.printf("%dª      %,16s %,16s %,16s  R$ %,18s%n",
                    i,
                    nf.format(totalOficial[i]),
                    nf.format(desbloqueados[i]),
                    nf.format(exclusao),
                    nf.format(valorRetido[i]));

            // GERA GRÁFICO DE ROSCA
            gerarRosca(i, desbloqueados[i], exclusao);
        }

        System.out.println("─────────────────────────────────────────────────────────────");
        System.out.printf("TOTAL   %,16s %,16s %,16s  R$ %,18s%n",
                nf.format(38812),
                nf.format(totalDesbloq),
                nf.format(38812 - totalDesbloq),
                nf.format(totalValor));
        System.out.println("════════════════════════════════════\n");
        System.out.println("Gráficos salvos na pasta do projeto:");
        System.out.println("   grafico_lote_1.png  grafico_lote_2.png");
        System.out.println("   grafico_lote_3.png  grafico_lote_4.png");
    }

    private static void gerarRosca(int lote, long desbloq, long bloqueado) throws IOException {
        DefaultPieDataset data = new DefaultPieDataset();
        data.setValue("Desbloqueados", desbloq);
        data.setValue("Ainda bloqueados", bloqueado);

        JFreeChart chart = ChartFactory.createRingChart(
                "Lote " + lote + " – Prova de Vida / Recadastro",
                data, true, true, false);

        File out = new File("grafico_lote_" + lote + ".png");
        ChartUtils.saveChartAsPNG(out, chart, 700, 500);
    }
}    private static void gerarPizza(long totalDesbloq, long totalBloqueado) throws IOException {
        DefaultPieDataset data = new DefaultPieDataset();
        data.setValue("Desbloqueados", totalDesbloq);
        data.setValue("Ainda bloqueados", totalBloqueado);

        JFreeChart chart = ChartFactory.createPieChart(
                "Total – Prova de Vida / Recadastro",
                data, true, true, false);

        File out = new File("grafico_total.png");
        ChartUtils.saveChartAsPNG(out, chart, 700, 500);
    }
}
        gerarPizza(totalDesbloq, 38812 - totalDesbloq);
        System.out.println("   grafico_total.png");
    }
