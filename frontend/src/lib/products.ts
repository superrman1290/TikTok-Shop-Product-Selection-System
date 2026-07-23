export type ProductStatus = "ACTIVE" | "INACTIVE" | "REMOVED";
export type ImportStatus = "PENDING" | "RUNNING" | "SUCCESS" | "PARTIAL_SUCCESS" | "FAILED" | "CANCELLED";

export interface PageData<T> {
  page: number;
  pageSize: number;
  total: number;
  items: T[];
}

export interface ProductSummary {
  id: number;
  externalProductId: string;
  platform: string;
  market: string;
  title: string;
  categoryId: number;
  categoryName: string;
  shopId: number;
  shopName: string;
  currency: string;
  currentPrice: number;
  originalPrice?: number;
  imageUrl?: string;
  productUrl?: string;
  rating?: number;
  reviewCount: number;
  listedAt?: string;
  collectedAt: string;
  status: ProductStatus;
}

export interface ProductDetail extends ProductSummary {
  categoryExternalId: string;
  shopExternalId: string;
  latestStatDate?: string;
}

export interface ProductStat {
  statDate: string;
  price: number;
  salesVolume: number;
  salesAmount: number;
  totalSalesVolume: number;
  videoCount: number;
  creatorCount: number;
  shopCount: number;
  similarProductCount: number;
  top10ShopSalesShare?: number;
  top10CreatorSalesShare?: number;
  negativeReviewRate?: number;
  reviewCount: number;
  rating?: number;
}

export type ScoreStatus = "DATA_INSUFFICIENT" | "PARTIAL" | "COMPLETE";
export type Recommendation = "RECOMMENDED" | "WATCH" | "NOT_RECOMMENDED" | "DATA_INSUFFICIENT";
export type LifecycleStage = "NEW" | "GROWTH" | "EXPLOSIVE" | "MATURE" | "DECLINE";

export interface ScoreDetail {
  metricCode: string;
  metricName: string;
  rawValue?: number;
  normalizedScore?: number;
  weight?: number;
  parentWeight?: number;
  overallContribution?: number;
  description: string;
}

export interface ProfitCalculation {
  sellingPrice: number;
  platformCommission: number;
  paymentFee: number;
  advertisingCost: number;
  refundLoss: number;
  totalCost: number;
  estimatedProfit: number;
  estimatedProfitMargin: number;
  costProfitMargin?: number;
}

export interface CostProfile {
  currency: string;
  purchaseCost: number;
  domesticShippingCost: number;
  internationalShippingCost: number;
  platformCommissionRate: number;
  paymentFeeRate: number;
  advertisingCostRate: number;
  refundLossRate: number;
  otherCost: number;
}

export interface ResolvedCostProfile {
  profile: CostProfile;
  source: "USER" | "MARKET";
}

export interface ProductAnalysis {
  productId: number;
  market: string;
  categoryId: number;
  analysisDate: string;
  algorithmVersion: string;
  scoreStatus: ScoreStatus;
  salesVolume7d?: number;
  salesGrowthRate7d?: number;
  salesGrowthRate30d?: number;
  videoGrowthRate7d?: number;
  creatorGrowthRate7d?: number;
  trendScore?: number;
  competitionScore?: number;
  estimatedProfit?: number;
  estimatedProfitMargin?: number;
  profitScore?: number;
  riskScore?: number;
  selectionScore?: number;
  lifecycleStage?: LifecycleStage;
  recommendation: Recommendation;
  scoreDetails: ScoreDetail[];
  reasons: string[];
  risks: string[];
}

export interface ImportJob {
  id: number;
  importType: "PRODUCT" | "PRODUCT_STAT";
  sourceType: "CSV" | "MOCK";
  status: ImportStatus;
  originalFileName: string;
  totalRows: number;
  successRows: number;
  failedRows: number;
  createdBy: number;
  createdByUsername: string;
  startedAt?: string;
  completedAt?: string;
  createdAt: string;
}

export interface ImportRowError {
  rowNumber: number;
  fieldName: string;
  rawValue?: string;
  errorCode: string;
  errorMessage: string;
}

export interface WatchlistItem {
  productId: number;
  title: string;
  market: string;
  currency: string;
  currentPrice: number;
  imageUrl?: string;
  selectionScore?: number;
  recommendation?: Recommendation;
  createdAt: string;
}

export interface AlertRule {
  id: number;
  productId: number;
  salesGrowth7dThreshold?: number;
  priceDrop7dThreshold?: number;
  competitionScoreIncreaseThreshold?: number;
  selectionScoreDropThreshold?: number;
  enabled: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface AlertRuleInput {
  productId: number;
  salesGrowth7dThreshold?: number;
  priceDrop7dThreshold?: number;
  competitionScoreIncreaseThreshold?: number;
  selectionScoreDropThreshold?: number;
  enabled: boolean;
}

export interface AlertEvent {
  id: number;
  productId: number;
  productTitle: string;
  market: string;
  alertRuleId: number;
  metricType: "SALES_GROWTH_7D" | "PRICE_DROP_7D" | "COMPETITION_SCORE_INCREASE" | "SELECTION_SCORE_DROP";
  statDate: string;
  metricValue: number;
  thresholdValue: number;
  readStatus: boolean;
  readAt?: string;
  createdAt: string;
}

export interface DashboardProduct {
  productId: number;
  title: string;
  market: string;
  currency: string;
  currentPrice: number;
  salesGrowthRate7d?: number;
  selectionScore?: number;
  recommendation?: Recommendation;
}

export interface UserDashboard {
  market: string;
  productCount: number;
  newProducts7d: number;
  recommendedProducts: number;
  watchlistProductCount: number;
  unreadAlertCount: number;
  topSalesGrowthProducts: DashboardProduct[];
  topSelectionScoreProducts: DashboardProduct[];
}

export interface AdminDashboard { userCount: number; enabledUserCount: number; productCount: number; newProducts7d: number; importJobs7d: number; importSuccessRate: number; pendingAnalysisJobs: number; failedAnalysisJobs: number; alerts7d: number; }
export interface AdminUser { id: number; email: string; username: string; role: "USER" | "ADMIN"; status: string; createdAt: string; }
export interface DataSourceRecord { id: number; name: string; sourceType: string; markets: string[]; endpointUrl?: string; secretHint?: string; enabled: boolean; createdAt: string; updatedAt: string; }
export interface AuditLog { id: number; operatorEmail?: string; actionType: string; targetType: string; targetId?: string; requestId?: string; requestIp?: string; result: string; detail?: string; createdAt: string; }

interface ApiEnvelope<T> {
  code: number;
  message: string;
  data: T;
}

export class ProductApiError extends Error {
  constructor(message: string, public readonly code: number, public readonly status: number) {
    super(message);
    this.name = "ProductApiError";
  }
}

async function request<T>(path: string, accessToken: string, init: RequestInit = {}): Promise<T> {
  const response = await fetch(path, {
    ...init,
    credentials: "include",
    headers: {
      Authorization: `Bearer ${accessToken}`,
      ...(!(init.body instanceof FormData) && init.body ? { "Content-Type": "application/json" } : {}),
      ...init.headers,
    },
  });
  const envelope = (await response.json()) as ApiEnvelope<T>;
  if (!response.ok || envelope.code !== 0) {
    throw new ProductApiError(envelope.message || "请求失败", envelope.code, response.status);
  }
  return envelope.data;
}

function queryString(values: Record<string, string | number | undefined>) {
  const params = new URLSearchParams();
  Object.entries(values).forEach(([key, value]) => {
    if (value !== undefined && value !== "") params.set(key, String(value));
  });
  return params.toString();
}

export const productApi = {
  list(accessToken: string, params: Record<string, string | number | undefined>) {
    return request<PageData<ProductSummary>>(`/api/v1/products?${queryString(params)}`, accessToken);
  },
  get(accessToken: string, productId: number) {
    return request<ProductDetail>(`/api/v1/products/${productId}`, accessToken);
  },
  stats(accessToken: string, productId: number) {
    return request<ProductStat[]>(`/api/v1/products/${productId}/stats`, accessToken);
  },
  analysis(accessToken: string, productId: number) {
    return request<ProductAnalysis>(`/api/v1/products/${productId}/analysis`, accessToken);
  },
  costProfile(accessToken: string, productId: number) {
    return request<ResolvedCostProfile>(`/api/v1/products/${productId}/cost-profile`, accessToken);
  },
  saveCostProfile(accessToken: string, productId: number, profile: CostProfile) {
    return request<CostProfile>(`/api/v1/products/${productId}/cost-profile`, accessToken, {
      method: "PUT",
      body: JSON.stringify(profile),
    });
  },
  deleteCostProfile(accessToken: string, productId: number) {
    return request<void>(`/api/v1/products/${productId}/cost-profile`, accessToken, { method: "DELETE" });
  },
  profitCalculation(accessToken: string, productId: number, sellingPrice: number, costProfile?: CostProfile) {
    return request<ProfitCalculation>(`/api/v1/products/${productId}/profit-calculations`, accessToken, {
      method: "POST",
      body: JSON.stringify({ sellingPrice, costProfile }),
    });
  },
  adminProducts(accessToken: string, params: Record<string, string | number | undefined>) {
    return request<PageData<ProductSummary>>(`/api/v1/admin/products?${queryString(params)}`, accessToken);
  },
  updateProduct(accessToken: string, productId: number, input: {
    title: string;
    currentPrice: number;
    originalPrice?: number;
    imageUrl?: string;
    productUrl?: string;
    status: ProductStatus;
  }) {
    return request<ProductDetail>(`/api/v1/admin/products/${productId}`, accessToken, {
      method: "PUT",
      body: JSON.stringify(input),
    });
  },
  imports(accessToken: string, page = 1) {
    return request<PageData<ImportJob>>(`/api/v1/admin/imports?page=${page}&pageSize=20`, accessToken);
  },
  importErrors(accessToken: string, importId: number) {
    return request<PageData<ImportRowError>>(`/api/v1/admin/imports/${importId}/errors?page=1&pageSize=100`, accessToken);
  },
  upload(accessToken: string, type: "products" | "product-stats", file: File) {
    const body = new FormData();
    body.append("file", file);
    return request<ImportJob>(`/api/v1/admin/imports/${type}`, accessToken, { method: "POST", body });
  },
};

export const userWorkflowApi = {
  dashboard(accessToken: string, market: string) {
    return request<UserDashboard>(`/api/v1/dashboard?${queryString({ market })}`, accessToken);
  },
  watchlist(accessToken: string, params: Record<string, string | number | undefined> = {}) {
    return request<PageData<WatchlistItem>>(`/api/v1/watchlist?${queryString(params)}`, accessToken);
  },
  addWatchlist(accessToken: string, productId: number) {
    return request<void>(`/api/v1/watchlist/${productId}`, accessToken, { method: "POST" });
  },
  removeWatchlist(accessToken: string, productId: number) {
    return request<void>(`/api/v1/watchlist/${productId}`, accessToken, { method: "DELETE" });
  },
  alertRules(accessToken: string) {
    return request<AlertRule[]>("/api/v1/alert-rules", accessToken);
  },
  createAlertRule(accessToken: string, input: AlertRuleInput) {
    return request<AlertRule>("/api/v1/alert-rules", accessToken, { method: "POST", body: JSON.stringify(input) });
  },
  updateAlertRule(accessToken: string, ruleId: number, input: AlertRuleInput) {
    return request<AlertRule>(`/api/v1/alert-rules/${ruleId}`, accessToken, { method: "PUT", body: JSON.stringify(input) });
  },
  deleteAlertRule(accessToken: string, ruleId: number) {
    return request<void>(`/api/v1/alert-rules/${ruleId}`, accessToken, { method: "DELETE" });
  },
  alerts(accessToken: string, params: Record<string, string | number | boolean | undefined>) {
    return request<PageData<AlertEvent>>(`/api/v1/alerts?${queryString(params as Record<string, string | number | undefined>)}`, accessToken);
  },
  markAlertRead(accessToken: string, alertId: number) {
    return request<void>(`/api/v1/alerts/${alertId}/read`, accessToken, { method: "POST" });
  },
  markAllAlertsRead(accessToken: string) {
    return request<number>("/api/v1/alerts/read-all", accessToken, { method: "POST" });
  },
};

export const adminApi = {
  dashboard: (token: string) => request<AdminDashboard>("/api/v1/admin/dashboard", token),
  users: (token: string) => request<PageData<AdminUser>>("/api/v1/admin/users?page=1&pageSize=100", token),
  changeRole: (token: string, id: number, role: "USER" | "ADMIN") => request<AdminUser>(`/api/v1/admin/users/${id}/role`, token, { method: "PUT", body: JSON.stringify({ role }) }),
  dataSources: (token: string) => request<DataSourceRecord[]>("/api/v1/admin/data-sources", token),
  saveDataSource: (token: string, input: Omit<DataSourceRecord, "id" | "createdAt" | "updatedAt" | "secretHint"> & { secret?: string }, id?: number) => request<DataSourceRecord>(`/api/v1/admin/data-sources${id ? `/${id}` : ""}`, token, { method: id ? "PUT" : "POST", body: JSON.stringify(input) }),
  dataSourceStatus: (token: string, id: number, enabled: boolean) => request<DataSourceRecord>(`/api/v1/admin/data-sources/${id}/status?enabled=${enabled}`, token, { method: "PUT" }),
  analysisJobs: (token: string) => request<PageData<{ id: number; productId: number; analysisDate: string; algorithmVersion: string; status: string; attemptCount: number }>>("/api/v1/admin/jobs/analysis?page=1&pageSize=100", token),
  recalculate: (token: string) => request<number>("/api/v1/admin/jobs/analysis/recalculate", token, { method: "POST" }),
  auditLogs: (token: string) => request<PageData<AuditLog>>("/api/v1/admin/audit-logs?page=1&pageSize=100", token),
};

export const markets = ["US", "GB", "TH", "VN", "PH", "MY", "SG", "ID"];
