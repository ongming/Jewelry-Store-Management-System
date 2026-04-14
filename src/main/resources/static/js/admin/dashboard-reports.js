(() => {
    const STATUS_OUT = "out";
    const STATUS_LOW = "low";
    const STATUS_OK = "ok";
    const dashboardData = window.dashboardData || {};
    const rawData = {
        orders: Array.isArray(dashboardData.orders) ? dashboardData.orders : [],
        orderDetails: Array.isArray(dashboardData.orderDetails) ? dashboardData.orderDetails : [],
        inventory: Array.isArray(dashboardData.inventory) ? dashboardData.inventory : []
    };

    const ui = {
        form: document.getElementById("reportFilterForm"),
        reportType: document.getElementById("reportType"),
        timeType: document.getElementById("timeType"),
        startDate: document.getElementById("startDate"),
        endDate: document.getElementById("endDate"),
        kpiGrid: document.getElementById("kpiGrid"),
        primaryTitle: document.getElementById("primaryChartTitle"),
        primarySubtitle: document.getElementById("primaryChartSubtitle"),
        secondaryTitle: document.getElementById("secondaryChartTitle"),
        secondarySubtitle: document.getElementById("secondaryChartSubtitle"),
        primaryCanvas: document.getElementById("primaryChart"),
        secondaryCanvas: document.getElementById("secondaryChart"),
        tableTitle: document.getElementById("tableTitle"),
        tableSubtitle: document.getElementById("tableSubtitle"),
        tableWrap: document.getElementById("tableWrap"),
        tableHead: document.getElementById("detailTableHead"),
        tableBody: document.getElementById("detailTableBody"),
        emptyState: document.getElementById("emptyState"),
        tableFooter: document.getElementById("tableFooter"),
        prevPageBtn: document.getElementById("prevPageBtn"),
        nextPageBtn: document.getElementById("nextPageBtn"),
        pageInfo: document.getElementById("pageInfo"),
        loadingOverlay: document.getElementById("loadingOverlay")
    };

    const tableState = {
        rows: [],
        columns: [],
        sortKey: "",
        sortDir: "desc",
        page: 1,
        pageSize: 8,
        highlightKey: ""
    };

    const chartState = {
        primary: null,
        secondary: null
    };

    const reportService = {
        generateReport(reportType, timeType, startDate, endDate) {
            return new Promise(resolve => {
                setTimeout(() => {
                    resolve(generateRealResult(reportType, timeType, startDate, endDate));
                }, 220);
            });
        }
    };

    window.reportService = reportService;

    function toISODate(date) {
        return date.toISOString().slice(0, 10);
    }

    function parseDateValue(value, fallbackDate) {
        if (!value) {
            return new Date(fallbackDate);
        }
        if (value instanceof Date) {
            const copy = new Date(value);
            return Number.isNaN(copy.getTime()) ? new Date(fallbackDate) : copy;
        }
        const stringValue = value.toString();
        const date = stringValue.includes("T")
            ? new Date(stringValue)
            : new Date(stringValue + "T00:00:00");
        return Number.isNaN(date.getTime()) ? new Date(fallbackDate) : date;
    }

    function dayStart(date) {
        const value = new Date(date);
        value.setHours(0, 0, 0, 0);
        return value;
    }

    function dayEnd(date) {
        const value = new Date(date);
        value.setHours(23, 59, 59, 999);
        return value;
    }

    function getDataBounds() {
        const sources = [];

        rawData.orders.forEach(order => {
            const date = parseDateValue(order.orderDate, new Date());
            if (!Number.isNaN(date.getTime())) {
                sources.push(date);
            }
        });

        rawData.orderDetails.forEach(detail => {
            const date = parseDateValue(detail.orderDate, new Date());
            if (!Number.isNaN(date.getTime())) {
                sources.push(date);
            }
        });

        if (!sources.length) {
            const today = dayStart(new Date());
            return { min: today, max: today };
        }

        const min = dayStart(new Date(Math.min(...sources.map(item => item.getTime()))));
        const max = dayStart(new Date(Math.max(...sources.map(item => item.getTime()))));
        return { min, max };
    }

    function normalizeRange(startValue, endValue) {
        const bounds = getDataBounds();
        const start = dayStart(parseDateValue(startValue, bounds.min));
        const end = dayStart(parseDateValue(endValue, bounds.max));
        if (start.getTime() <= end.getTime()) {
            return { start, end };
        }
        return { start: end, end: start };
    }

    function isCancelledStatus(status) {
        if (!status) {
            return false;
        }
        return status.toString().trim().toUpperCase() === "CANCELLED";
    }

    function inDateRange(dateValue, start, end) {
        const date = parseDateValue(dateValue, start);
        return date.getTime() >= dayStart(start).getTime() && date.getTime() <= dayEnd(end).getTime();
    }

    function pad2(value) {
        return String(value).padStart(2, "0");
    }

    function bucketForDate(dateValue, timeType) {
        const date = parseDateValue(dateValue, new Date());
        const year = date.getFullYear();
        const month = date.getMonth() + 1;
        const day = date.getDate();

        if (timeType === "year") {
            return {
                key: String(year),
                label: String(year),
                sortValue: year * 10000
            };
        }

        if (timeType === "month") {
            return {
                key: `${year}-${pad2(month)}`,
                label: `${pad2(month)}/${year}`,
                sortValue: year * 100 + month
            };
        }

        return {
            key: `${year}-${pad2(month)}-${pad2(day)}`,
            label: `${pad2(day)}/${pad2(month)}/${year}`,
            sortValue: year * 10000 + month * 100 + day
        };
    }

    function buildRangeBuckets(timeType, start, end) {
        const buckets = [];
        if (timeType === "year") {
            for (let year = start.getFullYear(); year <= end.getFullYear(); year += 1) {
                buckets.push({
                    key: String(year),
                    label: String(year),
                    sortValue: year * 10000
                });
            }
            return buckets;
        }

        if (timeType === "month") {
            const cursor = new Date(start.getFullYear(), start.getMonth(), 1);
            const last = new Date(end.getFullYear(), end.getMonth(), 1);
            while (cursor.getTime() <= last.getTime()) {
                const year = cursor.getFullYear();
                const month = cursor.getMonth() + 1;
                buckets.push({
                    key: `${year}-${pad2(month)}`,
                    label: `${pad2(month)}/${year}`,
                    sortValue: year * 100 + month
                });
                cursor.setMonth(cursor.getMonth() + 1);
            }
            return buckets;
        }

        const cursor = dayStart(start);
        const last = dayStart(end);
        while (cursor.getTime() <= last.getTime()) {
            const bucket = bucketForDate(cursor, "day");
            buckets.push(bucket);
            cursor.setDate(cursor.getDate() + 1);
        }
        return buckets;
    }

    function formatCurrency(value) {
        const amount = Number.isFinite(value) ? value : 0;
        return new Intl.NumberFormat("vi-VN").format(Math.round(amount)) + " VND";
    }

    function formatNumber(value) {
        const amount = Number.isFinite(value) ? value : 0;
        return new Intl.NumberFormat("vi-VN").format(Math.round(amount));
    }

    function formatPercent(value) {
        const amount = Number.isFinite(value) ? value : 0;
        const sign = amount > 0 ? "+" : "";
        return sign + amount.toFixed(1) + "%";
    }

    function generateRealResult(reportType, timeType, startDate, endDate) {
        if (reportType === "top-products") {
            return generateProductReportResult(timeType, startDate, endDate);
        }
        if (reportType === "inventory") {
            return generateInventoryReportResult(timeType, startDate, endDate);
        }
        return generateRevenueReportResult(timeType, startDate, endDate);
    }

    function generateRevenueReportResult(timeType, startDate, endDate) {
        const range = normalizeRange(startDate, endDate);
        const buckets = buildRangeBuckets(timeType, range.start, range.end);
        const bucketMap = new Map();
        buckets.forEach(bucket => {
            bucketMap.set(bucket.key, {
                period: bucket.label,
                sortValue: bucket.sortValue,
                revenue: 0,
                orders: 0
            });
        });

        rawData.orders.forEach(order => {
            const orderDate = parseDateValue(order.orderDate, range.start);
            if (!inDateRange(orderDate, range.start, range.end)) {
                return;
            }
            if (isCancelledStatus(order.status)) {
                return;
            }
            const bucket = bucketForDate(orderDate, timeType);
            if (!bucketMap.has(bucket.key)) {
                bucketMap.set(bucket.key, {
                    period: bucket.label,
                    sortValue: bucket.sortValue,
                    revenue: 0,
                    orders: 0
                });
            }
            const target = bucketMap.get(bucket.key);
            const amount = Number(order.finalTotal) || 0;
            target.revenue += amount;
            target.orders += 1;
        });

        const rows = Array.from(bucketMap.values())
            .filter(item => item.revenue > 0 || item.orders > 0)
            .sort((a, b) => a.sortValue - b.sortValue)
            .map(item => ({
                period: item.period,
                revenue: Math.round(item.revenue),
                orders: item.orders
            }));

        const totalRevenue = rows.reduce((sum, row) => sum + row.revenue, 0);
        const totalOrders = rows.reduce((sum, row) => sum + row.orders, 0);
        const averageOrderValue = totalOrders > 0 ? totalRevenue / totalOrders : 0;

        const first = rows[0]?.revenue ?? 0;
        const last = rows[rows.length - 1]?.revenue ?? 0;
        const trendPercent = first > 0 ? ((last - first) / first) * 100 : 0;

        return {
            resultType: "RevenueReportResult",
            reportType: "revenue",
            timeType,
            startDate,
            endDate,
            kpis: {
                totalRevenue,
                totalOrders,
                averageOrderValue,
                trendPercent
            },
            charts: {
                primary: {
                    type: "line",
                    title: "Doanh thu theo thời gian",
                    subtitle: "Line chart",
                    labels: rows.map(row => row.period),
                    values: rows.map(row => row.revenue),
                    valueKind: "currency"
                },
                secondary: {
                    type: "bar",
                    title: "Số đơn theo thời gian",
                    subtitle: "Bar chart",
                    labels: rows.map(row => row.period),
                    values: rows.map(row => row.orders),
                    valueKind: "number"
                }
            },
            table: {
                title: "Chi tiết doanh thu",
                subtitle: "Ngày | Doanh thu | Số đơn",
                columns: [
                    { key: "period", label: "Ngày" },
                    { key: "revenue", label: "Doanh thu", type: "currency" },
                    { key: "orders", label: "Số đơn", type: "number" }
                ],
                rows,
                sortKey: "revenue",
                highlightKey: "revenue"
            }
        };
    }

    function generateProductReportResult(timeType, startDate, endDate) {
        const range = normalizeRange(startDate, endDate);
        const grouped = new Map();

        rawData.orderDetails.forEach(detail => {
            const orderDate = parseDateValue(detail.orderDate, range.start);
            if (!inDateRange(orderDate, range.start, range.end)) {
                return;
            }
            if (isCancelledStatus(detail.orderStatus)) {
                return;
            }

            const productName = (detail.productName || "Unknown").toString();
            const quantity = Number(detail.quantity) || 0;
            const unitPrice = Number(detail.unitPrice) || 0;
            const lineRevenue = Number(detail.revenue) || unitPrice * quantity;

            if (!grouped.has(productName)) {
                grouped.set(productName, {
                    productName,
                    quantity: 0,
                    revenue: 0
                });
            }
            const target = grouped.get(productName);
            target.quantity += quantity;
            target.revenue += lineRevenue;
        });

        const rows = Array.from(grouped.values())
            .map(item => ({
                productName: item.productName,
                quantity: item.quantity,
                revenue: Math.round(item.revenue)
            }))
            .sort((a, b) => b.revenue - a.revenue);

        const totalRevenue = rows.reduce((sum, row) => sum + row.revenue, 0);
        const top = rows[0] || { productName: "-", quantity: 0, revenue: 0 };
        const contribution = totalRevenue > 0 ? (top.revenue / totalRevenue) * 100 : 0;

        const topRows = rows.slice(0, 6);

        return {
            resultType: "ProductReportResult",
            reportType: "top-products",
            timeType,
            startDate,
            endDate,
            kpis: {
                topProductName: top.productName,
                topQuantity: top.quantity,
                contributionPercent: contribution
            },
            charts: {
                primary: {
                    type: "pie",
                    title: "Tỷ trọng sản phẩm bán chạy",
                    subtitle: "Pie chart",
                    labels: topRows.map(row => row.productName),
                    values: topRows.map(row => row.quantity),
                    valueKind: "number"
                },
                secondary: {
                    type: "bar",
                    title: "Doanh thu theo sản phẩm",
                    subtitle: "Bar chart",
                    labels: topRows.map(row => row.productName),
                    values: topRows.map(row => row.revenue),
                    valueKind: "currency"
                }
            },
            table: {
                title: "Chi tiết top sản phẩm",
                subtitle: "Tên sản phẩm | Số lượng bán | Doanh thu",
                columns: [
                    { key: "productName", label: "Tên sản phẩm" },
                    { key: "quantity", label: "Số lượng bán", type: "number" },
                    { key: "revenue", label: "Doanh thu", type: "currency" }
                ],
                rows,
                sortKey: "revenue",
                highlightKey: "revenue"
            }
        };
    }

    function generateInventoryReportResult(timeType, startDate, endDate) {
        const rows = rawData.inventory.map(item => {
            const stock = Number(item.stock) || 0;
            let statusTone = STATUS_OK;
            let statusText = "Còn nhiều";

            if (stock === 0) {
                statusTone = STATUS_OUT;
                statusText = "Hết hàng";
            } else if (stock <= 12) {
                statusTone = STATUS_LOW;
                statusText = "Sắp hết";
            }

            return {
                productName: (item.productName || "Unknown").toString(),
                stock,
                statusTone,
                statusText
            };
        }).sort((a, b) => a.stock - b.stock);

        const outOfStock = rows.filter(row => row.statusTone === STATUS_OUT).length;
        const lowStock = rows.filter(row => row.statusTone === STATUS_LOW).length;
        const healthy = rows.filter(row => row.statusTone === STATUS_OK).length;

        return {
            resultType: "InventoryReportResult",
            reportType: "inventory",
            timeType,
            startDate,
            endDate,
            kpis: {
                lowStock,
                outOfStock,
                healthy
            },
            charts: {
                primary: {
                    type: "bar",
                    title: "Số lượng tồn theo sản phẩm",
                    subtitle: "Bar chart",
                    labels: rows.map(row => row.productName),
                    values: rows.map(row => row.stock),
                    valueKind: "number"
                },
                secondary: {
                    type: "pie",
                    title: "Phân bố trạng thái tồn kho",
                    subtitle: "Pie chart",
                    labels: ["Hết hàng", "Sắp hết", "Còn nhiều"],
                    values: [outOfStock, lowStock, healthy],
                    valueKind: "number"
                }
            },
            table: {
                title: "Chi tiết tồn kho",
                subtitle: "Tên sản phẩm | Số lượng tồn | Trạng thái",
                columns: [
                    { key: "productName", label: "Tên sản phẩm" },
                    { key: "stock", label: "Số lượng tồn", type: "number" },
                    { key: "statusText", label: "Trạng thái", type: "status" }
                ],
                rows,
                sortKey: "stock",
                highlightKey: "stock"
            }
        };
    }

    function mapRevenueReportResult(result) {
        return {
            kpis: [
                {
                    icon: "fa-solid fa-money-bill-wave",
                    tone: "gold",
                    label: "Tổng doanh thu",
                    value: formatCurrency(result.kpis.totalRevenue),
                    trend: {
                        text: formatPercent(result.kpis.trendPercent),
                        tone: result.kpis.trendPercent >= 0 ? "up" : "down"
                    }
                },
                {
                    icon: "fa-solid fa-receipt",
                    tone: "blue",
                    label: "Tổng đơn hàng",
                    value: formatNumber(result.kpis.totalOrders),
                    trend: {
                        text: "Theo bộ lọc",
                        tone: "neutral"
                    }
                },
                {
                    icon: "fa-solid fa-chart-line",
                    tone: "gold",
                    label: "Giá trị trung bình / đơn",
                    value: formatCurrency(result.kpis.averageOrderValue),
                    trend: {
                        text: "AOV",
                        tone: "neutral"
                    }
                }
            ],
            primaryChart: result.charts.primary,
            secondaryChart: result.charts.secondary,
            table: result.table
        };
    }

    function mapProductReportResult(result) {
        return {
            kpis: [
                {
                    icon: "fa-solid fa-crown",
                    tone: "gold",
                    label: "Top sản phẩm",
                    value: result.kpis.topProductName,
                    trend: {
                        text: "Top 1",
                        tone: "neutral"
                    }
                },
                {
                    icon: "fa-solid fa-box-open",
                    tone: "blue",
                    label: "Số lượng bán cao nhất",
                    value: formatNumber(result.kpis.topQuantity),
                    trend: {
                        text: "Sản phẩm / kỳ",
                        tone: "neutral"
                    }
                },
                {
                    icon: "fa-solid fa-percent",
                    tone: "gold",
                    label: "Tỷ lệ đóng góp",
                    value: formatPercent(result.kpis.contributionPercent),
                    trend: {
                        text: "Doanh thu top 1",
                        tone: "neutral"
                    }
                }
            ],
            primaryChart: result.charts.primary,
            secondaryChart: result.charts.secondary,
            table: result.table
        };
    }

    function mapInventoryReportResult(result) {
        return {
            kpis: [
                {
                    icon: "fa-solid fa-triangle-exclamation",
                    tone: "warn",
                    label: "Sản phẩm sắp hết",
                    value: formatNumber(result.kpis.lowStock),
                    trend: {
                        text: "Cần nhập thêm",
                        tone: result.kpis.lowStock > 0 ? "down" : "neutral"
                    }
                },
                {
                    icon: "fa-solid fa-circle-xmark",
                    tone: "warn",
                    label: "Hết hàng",
                    value: formatNumber(result.kpis.outOfStock),
                    trend: {
                        text: result.kpis.outOfStock > 0 ? "Cảnh báo" : "Ổn định",
                        tone: result.kpis.outOfStock > 0 ? "down" : "up"
                    }
                },
                {
                    icon: "fa-solid fa-boxes-stacked",
                    tone: "blue",
                    label: "Sản phẩm tồn ổn định",
                    value: formatNumber(result.kpis.healthy),
                    trend: {
                        text: "Còn nhiều",
                        tone: "up"
                    }
                }
            ],
            primaryChart: result.charts.primary,
            secondaryChart: result.charts.secondary,
            table: result.table
        };
    }

    function mapBackendResultToViewModel(result) {
        if (!result || !result.resultType) {
            return {
                kpis: [],
                primaryChart: {
                    type: "line",
                    title: "Biểu đồ chính",
                    subtitle: "Không có dữ liệu",
                    labels: [],
                    values: [],
                    valueKind: "number"
                },
                secondaryChart: {
                    type: "bar",
                    title: "Biểu đồ phụ",
                    subtitle: "Không có dữ liệu",
                    labels: [],
                    values: [],
                    valueKind: "number"
                },
                table: {
                    title: "Bảng dữ liệu",
                    subtitle: "Không có dữ liệu",
                    columns: [],
                    rows: [],
                    sortKey: "",
                    highlightKey: ""
                }
            };
        }

        if (result.resultType === "RevenueReportResult") {
            return mapRevenueReportResult(result);
        }
        if (result.resultType === "ProductReportResult") {
            return mapProductReportResult(result);
        }
        return mapInventoryReportResult(result);
    }

    function buildKpiCard(card) {
        return `
            <article class="kpi-card">
                <div class="kpi-icon ${card.tone}"><i class="${card.icon}"></i></div>
                <div class="kpi-body">
                    <p>${card.label}</p>
                    <div class="kpi-value">${card.value}</div>
                    <span class="kpi-trend ${card.trend.tone}">${card.trend.text}</span>
                </div>
            </article>
        `;
    }

    function renderKpis(kpis) {
        ui.kpiGrid.innerHTML = kpis.map(buildKpiCard).join("");
    }

    function chartColors(index) {
        const palette = ["#d4af37", "#56b5e9", "#37c28a", "#e7ba45", "#c67fe6", "#ff8d8d", "#85d7b2"];
        return palette[index % palette.length];
    }

    function getTooltipFormatter(kind) {
        if (kind === "currency") {
            return value => formatCurrency(value);
        }
        return value => formatNumber(value);
    }

    function buildChartConfig(chartData) {
        const tooltipFormatter = getTooltipFormatter(chartData.valueKind);

        if (chartData.type === "line") {
            return {
                type: "line",
                data: {
                    labels: chartData.labels,
                    datasets: [{
                        label: chartData.title,
                        data: chartData.values,
                        borderColor: "#d4af37",
                        backgroundColor: "rgba(212, 175, 55, 0.2)",
                        fill: true,
                        tension: 0.35,
                        pointRadius: 3,
                        pointHoverRadius: 5
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    animation: {
                        duration: 480,
                        easing: "easeOutQuart"
                    },
                    plugins: {
                        legend: { display: false },
                        tooltip: {
                            callbacks: {
                                label(context) {
                                    return tooltipFormatter(context.parsed.y);
                                }
                            }
                        }
                    },
                    scales: {
                        x: {
                            ticks: { color: "#bdbdbd" },
                            grid: { color: "rgba(255, 255, 255, 0.06)" }
                        },
                        y: {
                            ticks: {
                                color: "#bdbdbd",
                                callback(value) {
                                    return chartData.valueKind === "currency" ? formatCurrency(value) : formatNumber(value);
                                }
                            },
                            grid: { color: "rgba(255, 255, 255, 0.06)" }
                        }
                    }
                }
            };
        }

        if (chartData.type === "bar") {
            return {
                type: "bar",
                data: {
                    labels: chartData.labels,
                    datasets: [{
                        label: chartData.title,
                        data: chartData.values,
                        borderRadius: 6,
                        backgroundColor: chartData.values.map((_, index) => chartColors(index))
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    animation: {
                        duration: 480,
                        easing: "easeOutQuart"
                    },
                    plugins: {
                        legend: { display: false },
                        tooltip: {
                            callbacks: {
                                label(context) {
                                    return tooltipFormatter(context.parsed.y);
                                }
                            }
                        }
                    },
                    scales: {
                        x: {
                            ticks: { color: "#bdbdbd" },
                            grid: { color: "rgba(255, 255, 255, 0.03)" }
                        },
                        y: {
                            ticks: {
                                color: "#bdbdbd",
                                callback(value) {
                                    return chartData.valueKind === "currency" ? formatCurrency(value) : formatNumber(value);
                                }
                            },
                            grid: { color: "rgba(255, 255, 255, 0.06)" }
                        }
                    }
                }
            };
        }

        return {
            type: "pie",
            data: {
                labels: chartData.labels,
                datasets: [{
                    data: chartData.values,
                    backgroundColor: chartData.values.map((_, index) => chartColors(index))
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                animation: {
                    duration: 480,
                    easing: "easeOutQuart"
                },
                plugins: {
                    legend: {
                        position: "bottom",
                        labels: { color: "#bdbdbd", padding: 12 }
                    },
                    tooltip: {
                        callbacks: {
                            label(context) {
                                return context.label + ": " + tooltipFormatter(context.parsed);
                            }
                        }
                    }
                }
            }
        };
    }

    function renderCharts(viewModel) {
        ui.primaryTitle.textContent = viewModel.primaryChart.title;
        ui.primarySubtitle.textContent = viewModel.primaryChart.subtitle;
        ui.secondaryTitle.textContent = viewModel.secondaryChart.title;
        ui.secondarySubtitle.textContent = viewModel.secondaryChart.subtitle;

        if (chartState.primary) {
            chartState.primary.destroy();
        }
        if (chartState.secondary) {
            chartState.secondary.destroy();
        }

        chartState.primary = new Chart(ui.primaryCanvas, buildChartConfig(viewModel.primaryChart));
        chartState.secondary = new Chart(ui.secondaryCanvas, buildChartConfig(viewModel.secondaryChart));
    }

    function compareValues(a, b, dir) {
        if (typeof a === "number" && typeof b === "number") {
            return dir === "asc" ? a - b : b - a;
        }
        const aText = (a ?? "").toString().toLowerCase();
        const bText = (b ?? "").toString().toLowerCase();
        if (dir === "asc") {
            return aText.localeCompare(bText, "vi");
        }
        return bText.localeCompare(aText, "vi");
    }

    function sortRows(rows, key, dir) {
        if (!key) {
            return rows;
        }
        return [...rows].sort((left, right) => compareValues(left[key], right[key], dir));
    }

    function renderTableHeader() {
        ui.tableHead.innerHTML = "";
        const row = document.createElement("tr");

        tableState.columns.forEach(column => {
            const th = document.createElement("th");
            th.dataset.key = column.key;
            const direction = tableState.sortKey === column.key ? tableState.sortDir : "";
            th.textContent = column.label + (direction ? (direction === "asc" ? " ↑" : " ↓") : "");
            th.addEventListener("click", () => {
                if (tableState.sortKey === column.key) {
                    tableState.sortDir = tableState.sortDir === "asc" ? "desc" : "asc";
                } else {
                    tableState.sortKey = column.key;
                    tableState.sortDir = "desc";
                }
                tableState.page = 1;
                renderTableBody();
            });
            row.appendChild(th);
        });

        ui.tableHead.appendChild(row);
    }

    function renderCell(column, value, row, highlightValue) {
        if (column.type === "currency") {
            const css = value === highlightValue ? "top-value" : "";
            return `<span class="${css}">${formatCurrency(value)}</span>`;
        }
        if (column.type === "number") {
            const css = value === highlightValue ? "top-value" : "";
            return `<span class="${css}">${formatNumber(value)}</span>`;
        }
        if (column.type === "status") {
            const tone = row.statusTone || STATUS_OK;
            return `<span class="status-pill status-${tone}">${value}</span>`;
        }
        return value ?? "";
    }

    function renderTableBody() {
        const sortedRows = sortRows(tableState.rows, tableState.sortKey, tableState.sortDir);
        const totalPages = Math.max(1, Math.ceil(sortedRows.length / tableState.pageSize));
        tableState.page = Math.min(tableState.page, totalPages);

        const start = (tableState.page - 1) * tableState.pageSize;
        const end = start + tableState.pageSize;
        const pageRows = sortedRows.slice(start, end);

        ui.tableBody.innerHTML = "";
        const highlightColumn = tableState.highlightKey;
        let highlightValue = null;
        if (highlightColumn) {
            highlightValue = sortedRows.reduce((max, row) => {
                const value = Number(row[highlightColumn]) || 0;
                return Math.max(max, value);
            }, 0);
        }

        pageRows.forEach(row => {
            const tr = document.createElement("tr");
            tableState.columns.forEach(column => {
                const td = document.createElement("td");
                const value = row[column.key];
                td.innerHTML = renderCell(column, value, row, highlightValue);
                tr.appendChild(td);
            });
            ui.tableBody.appendChild(tr);
        });

        ui.pageInfo.textContent = tableState.page + " / " + totalPages;
        ui.prevPageBtn.disabled = tableState.page <= 1;
        ui.nextPageBtn.disabled = tableState.page >= totalPages;

        const hasData = sortedRows.length > 0;
        ui.tableWrap.hidden = !hasData;
        ui.tableFooter.hidden = !hasData;
        ui.emptyState.hidden = hasData;
    }

    function renderTable(table) {
        ui.tableTitle.textContent = table.title;
        ui.tableSubtitle.textContent = table.subtitle;

        tableState.columns = table.columns;
        tableState.rows = table.rows;
        tableState.sortKey = table.sortKey;
        tableState.sortDir = "desc";
        tableState.page = 1;
        tableState.highlightKey = table.highlightKey;

        renderTableHeader();
        renderTableBody();
    }

    function showLoading(visible) {
        if (!ui.loadingOverlay) {
            return;
        }
        ui.loadingOverlay.hidden = !visible;
    }

    function renderViewModel(viewModel) {
        renderKpis(viewModel.kpis);
        renderCharts(viewModel);
        renderTable(viewModel.table);
    }

    async function applyFilter() {
        const reportType = ui.reportType.value;
        const timeType = ui.timeType.value;
        const startDate = ui.startDate.value;
        const endDate = ui.endDate.value;

        if (startDate && endDate && startDate > endDate) {
            const temp = startDate;
            ui.startDate.value = endDate;
            ui.endDate.value = temp;
        }

        showLoading(true);
        try {
            const backendResult = await reportService.generateReport(reportType, timeType, ui.startDate.value, ui.endDate.value);
            const viewModel = mapBackendResultToViewModel(backendResult);
            renderViewModel(viewModel);
        } catch (error) {
            console.error("Failed to generate report", error);
            renderViewModel(mapBackendResultToViewModel(null));
        } finally {
            showLoading(false);
        }
    }

    function initFilterDefaults() {
        const bounds = getDataBounds();
        const end = dayStart(bounds.max);
        const start = dayStart(new Date(end));
        start.setDate(start.getDate() - 30);

        if (start.getTime() < bounds.min.getTime()) {
            ui.startDate.value = toISODate(bounds.min);
        } else {
            ui.startDate.value = toISODate(start);
        }
        ui.endDate.value = toISODate(end);
        ui.reportType.value = "revenue";
        ui.timeType.value = "day";
    }

    function bindEvents() {
        ui.form.addEventListener("submit", event => {
            event.preventDefault();
            applyFilter();
        });

        ui.prevPageBtn.addEventListener("click", () => {
            if (tableState.page > 1) {
                tableState.page -= 1;
                renderTableBody();
            }
        });

        ui.nextPageBtn.addEventListener("click", () => {
            const totalPages = Math.max(1, Math.ceil(tableState.rows.length / tableState.pageSize));
            if (tableState.page < totalPages) {
                tableState.page += 1;
                renderTableBody();
            }
        });
    }

    function init() {
        initFilterDefaults();
        bindEvents();
        applyFilter();
    }

    window.addEventListener("DOMContentLoaded", init);
})();
