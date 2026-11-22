package pk.edu.giki.microlend.products;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pk.edu.giki.microlend.common.Money;

/**
 * Golden-schedule tests (QR-02, risk R1): the generated schedule is compared, instalment by
 * instalment, with a reference schedule worked out independently of the code and stored under
 * {@code src/test/resources/golden/}.
 *
 * <p>Each file starts with one comment line naming the product and its inputs, then a CSV with
 * {@code sequence,due_date,principal,interest}. Disbursement is on the 31st so the due dates
 * also exercise the end-of-month clamping of BR-12. M2 holds one golden schedule per product;
 * the 30 required by QR-02 are an M3 target.</p>
 */
class GoldenScheduleTest {

    private static final String[] FILES = {
        "flat-rate-100000-12m.csv",
        "reducing-balance-100000-12m.csv",
        "bullet-100000-12m.csv",
    };

    @Test
    @DisplayName("flat-rate matches its golden schedule (TC-PRD-01.1)")
    void flatRate() throws IOException {
        check(FILES[0]);
    }

    @Test
    @DisplayName("reducing-balance matches its golden schedule (TC-PRD-01.2)")
    void reducingBalance() throws IOException {
        check(FILES[1]);
    }

    @Test
    @DisplayName("bullet matches its golden schedule (TC-PRD-01.3)")
    void bullet() throws IOException {
        check(FILES[2]);
    }

    private static void check(final String file) throws IOException {
        final List<String> lines = read(file);
        final Map<String, String> meta = header(lines.get(0));
        final Money principal = Money.of(new BigDecimal(meta.get("principal")));
        final int tenure = Integer.parseInt(meta.get("tenure"));
        final LoanProduct product = product(meta.get("product"), Integer.parseInt(meta.get("rate_bp")));
        final Schedule actual = product.generateSchedule(principal, tenure,
            LocalDate.parse(meta.get("disbursement")));

        assertEquals(tenure, lines.size() - 2, file + " golden rows");
        assertEquals(tenure, actual.instalments().size(), file + " generated rows");
        for (int i = 2; i < lines.size(); i++) {
            final String[] row = lines.get(i).split(",");
            final Instalment instalment = actual.instalments().get(i - 2);
            assertEquals(Integer.parseInt(row[0]), instalment.sequence(), file + " sequence");
            assertEquals(LocalDate.parse(row[1]), instalment.dueDate(), file + " due date " + row[0]);
            assertEquals(Money.of(new BigDecimal(row[2])), instalment.principal(),
                file + " principal " + row[0]);
            assertEquals(Money.of(new BigDecimal(row[3])), instalment.interest(),
                file + " interest " + row[0]);
        }
    }

    private static LoanProduct product(final String type, final int rateBp) {
        final Money min = Money.of(new BigDecimal("1000.00"));
        final Money max = Money.of(new BigDecimal("1000000.00"));
        if ("FLAT_RATE".equals(type)) {
            return new FlatRateProduct("GOLDEN", rateBp, min, max, 1, 36);
        }
        if ("REDUCING_BALANCE".equals(type)) {
            return new ReducingBalanceProduct("GOLDEN", rateBp, min, max, 1, 36);
        }
        return new BulletProduct("GOLDEN", rateBp, min, max, 1, 36);
    }

    private static Map<String, String> header(final String line) {
        final Map<String, String> meta = new HashMap<>();
        for (String pair : line.substring(1).trim().split(" ")) {
            final String[] kv = pair.split("=");
            meta.put(kv[0], kv[1]);
        }
        return meta;
    }

    private static List<String> read(final String file) throws IOException {
        try (InputStream in = GoldenScheduleTest.class.getResourceAsStream("/golden/" + file)) {
            if (in == null) {
                throw new IOException("golden file not found: " + file);
            }
            final BufferedReader reader =
                new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            final List<String> lines = new ArrayList<>();
            String line = reader.readLine();
            while (line != null) {
                if (!line.isBlank()) {
                    lines.add(line);
                }
                line = reader.readLine();
            }
            return lines;
        }
    }
}
