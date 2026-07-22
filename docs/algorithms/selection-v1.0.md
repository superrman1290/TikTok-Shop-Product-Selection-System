# selection-v1.0 golden cases

All cases are executed by `SelectionV1AlgorithmTest` and use `RoundingMode.HALF_UP` with two output decimals. Cost values are in the product's local market currency.

| Case | Input summary | Days | Cost condition | Expected result |
| --- | --- | ---: | --- | --- |
| G01 | Continuous sales history shorter than the minimum window | 13 | Market default | `DATA_INSUFFICIENT`; all selection scores are null; recommendation is `DATA_INSUFFICIENT` |
| G02 | Previous seven days sell 70 units, latest seven days sell 140 units | 14 | Market default | `PARTIAL`; 7-day growth `100.00`; no 30-day growth; trend score is calculated |
| G03 | Thirty continuous days with a first-day price of 30 and latest price of 27 | 30 | Market default | `COMPLETE`; 30-day growth and `PRICE_PRESSURE` are calculated from daily statistics |
| G04 | Previous seven-day sales are zero and latest seven-day sales are 70 | 14 | Market default | 7-day growth `7000.00`, using `max(previousSalesVolume7d, 1)` |
| G05 | 7-day growth is at least 80% and sales volume exceeds P75-derived threshold | 30 | Market default | Lifecycle `EXPLOSIVE` |
| G06 | Product age is exactly 14 days despite high growth | 14 | Market default | Lifecycle `NEW`, because NEW is evaluated before all other lifecycle rules |
| G07 | Selling price 29.99; published US default cost inputs | 30 | Market default | Estimated profit `9.32`; profit margin `31.09`; profit score is greater than zero |
| G08 | Purchase cost exceeds selling price | 30 | User cost override | Negative estimated profit and profit score `0.00` |
| G09 | Latest optional concentration and review metrics are absent | 30 | Market default | Competition and risk scores are still calculated from at least two metrics with re-normalized weights |
| G10 | Recommendation boundary values | 30 | Market default | `59.99 => NOT_RECOMMENDED`; `60.00 => WATCH`; `74.99 => WATCH`; `75.00 => RECOMMENDED` |

P75 contract coverage is executed by `BenchmarkServiceTest`: a 49-product sample does not participate and retains the fixed sales threshold of 100; a sorted 50-product sample uses nearest-rank `ceil(50 * 0.75) = 38`.

The tests additionally verify non-positive selling-price rejection and 30-day price-pressure handling. No random text or hidden score weights are used by the implementation.
