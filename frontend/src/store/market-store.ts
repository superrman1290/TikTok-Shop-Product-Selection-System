import { create } from "zustand";

interface MarketState {
  selectedMarket: string;
  setSelectedMarket: (market: string) => void;
}

export const useMarketStore = create<MarketState>((set) => ({
  selectedMarket: "US",
  setSelectedMarket: (market) => set({ selectedMarket: market }),
}));
