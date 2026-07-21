import { productApi } from "./products";

describe("productApi", () => {
  afterEach(() => vi.restoreAllMocks());

  it("builds bounded product query parameters and bearer authentication", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(JSON.stringify({
      code: 0,
      message: "success",
      data: { page: 1, pageSize: 20, total: 0, items: [] },
    }), { status: 200 }));

    await productApi.list("access-token", {
      page: 1,
      pageSize: 20,
      market: "US",
      keyword: undefined,
      sortBy: "rating",
      direction: "desc",
    });

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/products?page=1&pageSize=20&market=US&sortBy=rating&direction=desc",
      expect.objectContaining({
        credentials: "include",
        headers: expect.objectContaining({ Authorization: "Bearer access-token" }),
      }),
    );
  });

  it("does not set a JSON content type for multipart uploads", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(JSON.stringify({
      code: 0,
      message: "success",
      data: { id: 1 },
    }), { status: 200 }));
    await productApi.upload("token", "products", new File(["header"], "products.csv", { type: "text/csv" }));
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.body).toBeInstanceOf(FormData);
    expect(init.headers).not.toHaveProperty("Content-Type");
  });

  it("surfaces the unified API error code", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(JSON.stringify({
      code: 40004,
      message: "不支持的排序字段",
      data: null,
    }), { status: 400 }));
    await expect(productApi.list("token", { sortBy: "invalid" })).rejects.toMatchObject({
      code: 40004,
      status: 400,
    });
  });
});
