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

export const markets = ["US", "GB", "TH", "VN", "PH", "MY", "SG", "ID"];
