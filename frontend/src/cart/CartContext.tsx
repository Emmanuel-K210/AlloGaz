import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import type { SaleType } from '../api/types';

export interface CartLine {
  offerId: string;
  productId: string;
  productName: string;
  type: SaleType;
  quantity: number;
  unitPrice: number;
}

interface CartState {
  sellerId: string | null;
  sellerName: string | null;
  lines: CartLine[];
}

interface CartContextValue extends CartState {
  totalItems: number;
  totalAmount: number;
  addLine: (sellerId: string, sellerName: string, line: CartLine) => 'added' | 'replaced-seller';
  removeLine: (offerId: string, type: SaleType) => void;
  setQuantity: (offerId: string, type: SaleType, quantity: number) => void;
  clear: () => void;
}

const STORAGE_KEY = 'allogaz.cart';
const EMPTY: CartState = { sellerId: null, sellerName: null, lines: [] };
const CartContext = createContext<CartContextValue | null>(null);

function load(): CartState {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as CartState) : EMPTY;
  } catch {
    return EMPTY;
  }
}

export function CartProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<CartState>(load);

  useEffect(() => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
    } catch {
      // ignoré
    }
  }, [state]);

  const addLine = useCallback((sellerId: string, sellerName: string, line: CartLine) => {
    let outcome: 'added' | 'replaced-seller' = 'added';
    setState((prev) => {
      if (prev.sellerId && prev.sellerId !== sellerId) {
        outcome = 'replaced-seller';
        return { sellerId, sellerName, lines: [line] };
      }
      const existing = prev.lines.find((l) => l.offerId === line.offerId && l.type === line.type);
      const lines = existing
        ? prev.lines.map((l) =>
            l.offerId === line.offerId && l.type === line.type ? { ...l, quantity: l.quantity + line.quantity } : l,
          )
        : [...prev.lines, line];
      return { sellerId, sellerName, lines };
    });
    return outcome;
  }, []);

  const removeLine = useCallback((offerId: string, type: SaleType) => {
    setState((prev) => {
      const lines = prev.lines.filter((l) => !(l.offerId === offerId && l.type === type));
      return lines.length === 0 ? EMPTY : { ...prev, lines };
    });
  }, []);

  const setQuantity = useCallback((offerId: string, type: SaleType, quantity: number) => {
    setState((prev) => ({
      ...prev,
      lines: prev.lines.map((l) => (l.offerId === offerId && l.type === type ? { ...l, quantity } : l)),
    }));
  }, []);

  const clear = useCallback(() => setState(EMPTY), []);

  const value = useMemo<CartContextValue>(() => {
    const totalItems = state.lines.reduce((sum, l) => sum + l.quantity, 0);
    const totalAmount = state.lines.reduce((sum, l) => sum + l.quantity * l.unitPrice, 0);
    return { ...state, totalItems, totalAmount, addLine, removeLine, setQuantity, clear };
  }, [state, addLine, removeLine, setQuantity, clear]);

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>;
}

export function useCart(): CartContextValue {
  const ctx = useContext(CartContext);
  if (!ctx) throw new Error('useCart doit être utilisé dans <CartProvider>');
  return ctx;
}
