import { render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { FoundationStatus } from "./foundation-status";

function renderStatus() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <FoundationStatus />
    </QueryClientProvider>,
  );
}

describe("FoundationStatus", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("renders every healthy dependency", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(JSON.stringify({
      code: 0,
      message: "success",
      requestId: "test-request",
      timestamp: "2026-07-21T08:30:00Z",
      data: {
        application: "UP",
        database: "UP",
        redis: "UP",
        storage: "UP",
      },
    }), { status: 200 }));

    renderStatus();

    expect(await screen.findByText("Application")).toBeInTheDocument();
    expect(screen.getByText("Database")).toBeInTheDocument();
    expect(screen.getByText("Cache")).toBeInTheDocument();
    expect(screen.getByText("Object storage")).toBeInTheDocument();
    expect(screen.getAllByText("UP")).toHaveLength(4);
  });

  it("shows an error state when the endpoint is unavailable", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(null, { status: 503 }));

    renderStatus();

    await waitFor(() => {
      expect(screen.getByText("Health endpoint unavailable")).toBeInTheDocument();
    });
  });
});
