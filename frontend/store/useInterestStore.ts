import { create } from "zustand";
import { toast } from "react-toastify";
import { JwtToken } from "@/type/jwt";

export interface InterestGroup {
  groupId: string;
  groupName: string;
  groupSequence?: number;
  main?: boolean;
  memberId?: string;
}

export interface InterestStock {
  stockInfo: {
    stockName: string;
    stockCode: string;
    currentPrice: string;
    sign: string;
    changeAmount: string;
    changeRate: string;
    stockImage: string;
  };
  stockSequence: number;
}

export interface SearchResult {
  changeAmount: string;
  changeRate: string;
  currentPrice: string;
  sign: string;
  stockCode: string;
  stockName: string;
  stockImage: string;
}

interface InterestState {
  // 상태
  interestGroups: InterestGroup[];
  selectedGroupId: string | null;
  interestStocks: InterestStock[];
  isLoading: boolean;

  // 액션
  setInterestGroups: (groups: InterestGroup[]) => void;
  setSelectedGroupId: (groupId: string | null) => void;
  setInterestStocks: (stocks: InterestStock[]) => void;
  setIsLoading: (loading: boolean) => void;

  // API 호출 함수들
  fetchGroups: (token: JwtToken) => Promise<void>;
  fetchStocks: (token: JwtToken, groupId: string) => Promise<void>;
  addGroup: (token: JwtToken) => Promise<void>;
  updateGroupName: (
    token: JwtToken,
    groupId: string,
    groupName: string
  ) => Promise<void>;
  deleteGroup: (
    token: JwtToken,
    groupId: string,
    groupName: string
  ) => Promise<void>;
  setMainGroup: (
    token: JwtToken,
    groupId: string,
    showToast?: boolean
  ) => Promise<void>;
  addStock: (
    token: JwtToken,
    groupId: string,
    stock: SearchResult
  ) => Promise<void>;
  deleteStock: (
    token: JwtToken,
    groupId: string,
    stockCode: string
  ) => Promise<void>;
}

export const useInterestStore = create<InterestState>((set, get) => ({
  // 초기 상태
  interestGroups: [],
  selectedGroupId: null,
  interestStocks: [],
  isLoading: false,

  // 기본 액션들
  setInterestGroups: (groups) => set({ interestGroups: groups }),
  setSelectedGroupId: (groupId) => set({ selectedGroupId: groupId }),
  setInterestStocks: (stocks) => set({ interestStocks: stocks }),
  setIsLoading: (loading) => set({ isLoading: loading }),

  // API 호출 함수들
  fetchGroups: async (token: JwtToken) => {
    try {
      set({ isLoading: true });
      const res = await fetch(`/proxy/favorite/${token.memberId}`);
      const json: { data: InterestGroup[] } = await res.json();

      set({ interestGroups: json.data });

      // 그룹이 있으면 메인 그룹 또는 첫 번째 그룹을 선택
      if (json.data.length > 0) {
        const mainGroup = json.data.find((group) => group.main);
        const selectedId = mainGroup ? mainGroup.groupId : json.data[0].groupId;
        set({ selectedGroupId: selectedId });
      }
    } catch (error) {
      console.error("그룹 조회 실패:", error);
      toast.error("그룹 목록 조회 실패");
    } finally {
      set({ isLoading: false });
    }
  },

  fetchStocks: async (token: JwtToken, groupId: string) => {
    try {
      set({ isLoading: true });
      const res = await fetch(`/proxy/favorite/${token.memberId}/${groupId}`);
      const data: InterestStock[] = await res.json();

      if (res.ok) {
        set({ interestStocks: data });
      } else {
        toast.error("종목 목록 조회 실패");
      }
    } catch (error) {
      console.error("종목 조회 실패:", error);
      toast.error("종목 목록 조회 실패");
    } finally {
      set({ isLoading: false });
    }
  },

  addGroup: async (token: JwtToken) => {
    try {
      const randomId = Math.random().toString(36).substring(2, 10);
      const res = await fetch(`/proxy/favorite/${token.memberId}`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          groupName: `그룹명${randomId}`,
        }),
      });

      if (!res.ok) {
        toast.error("그룹 추가 실패");
        return;
      }

      const json = await res.json();
      toast.success("그룹이 추가되었습니다.");

      const newGroup: InterestGroup = {
        groupId: json.data.groupId,
        groupName: json.data.groupName,
        main: false,
        memberId: json.data.memberId,
        groupSequence: json.data.groupSequence,
      };

      const { interestGroups, selectedGroupId } = get();
      set({
        interestGroups: [...interestGroups, newGroup],
        // 기존 그룹이 없었던 경우에만 새 그룹을 선택
        selectedGroupId:
          interestGroups.length === 0 ? json.data.groupId : selectedGroupId,
        // 새 그룹이 선택된 경우에만 빈 종목 배열로 설정
        interestStocks: interestGroups.length === 0 ? [] : get().interestStocks,
      });

      // 첫 번째 그룹이면 메인으로 설정 (토스트 없이)
      if (interestGroups.length === 0) {
        await get().setMainGroup(token, json.data.groupId, false);
      }
    } catch (error) {
      console.error("그룹 추가 실패:", error);
      toast.error("그룹 추가 실패");
    }
  },

  updateGroupName: async (
    token: JwtToken,
    groupId: string,
    groupName: string
  ) => {
    try {
      const res = await fetch(`/proxy/favorite/${token.memberId}/${groupId}`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ groupName }),
      });

      if (!res.ok) {
        toast.error(`${groupName} 그룹명 수정 실패`);
        return;
      }

      toast.success("그룹명이 수정되었습니다.");

      const { interestGroups } = get();
      set({
        interestGroups: interestGroups.map((g) =>
          g.groupId === groupId ? { ...g, groupName } : g
        ),
      });
    } catch (error) {
      console.error("그룹명 수정 실패:", error);
      toast.error("그룹명 수정 실패");
    }
  },

  deleteGroup: async (token: JwtToken, groupId: string, groupName: string) => {
    try {
      const { selectedGroupId, interestStocks } = get();

      // 선택된 그룹의 종목들을 먼저 삭제
      if (selectedGroupId === groupId && interestStocks.length > 0) {
        for (const stock of interestStocks) {
          await fetch(
            `/proxy/favorite/${token.memberId}/${groupId}/stock?stockCode=${stock.stockInfo.stockCode}`,
            { method: "DELETE" }
          );
        }
        set({ interestStocks: [] });
      }

      // 그룹 삭제
      const deleteGroupRes = await fetch(
        `/proxy/favorite/${token.memberId}/${groupId}`,
        { method: "DELETE" }
      );

      if (!deleteGroupRes.ok) {
        toast.error(`${groupName} 그룹 삭제 실패`);
        return;
      }

      toast.success(`${groupName} 그룹이 삭제되었습니다.`);

      const { interestGroups } = get();
      const updatedGroups = interestGroups.filter((g) => g.groupId !== groupId);

      set({
        interestGroups: updatedGroups,
        selectedGroupId:
          updatedGroups.length > 0 ? updatedGroups[0].groupId : null,
      });
    } catch (error) {
      console.error("그룹 삭제 실패:", error);
      toast.error("그룹 삭제 실패");
    }
  },

  setMainGroup: async (
    token: JwtToken,
    groupId: string,
    showToast: boolean = true
  ) => {
    try {
      const res = await fetch(
        `/proxy/favorite/${token.memberId}/${groupId}/main`,
        { method: "PUT" }
      );

      if (!res.ok) {
        toast.error("메인 그룹 설정 실패");
        return;
      }

      if (showToast) {
        toast.success("메인 그룹이 설정되었습니다.");
      }

      // 기존 순서를 유지하면서 main 속성만 업데이트
      const { interestGroups } = get();
      const updatedGroups = interestGroups.map((group) => ({
        ...group,
        main: group.groupId === groupId,
      }));

      set({ interestGroups: updatedGroups });
    } catch (error) {
      console.error("메인 그룹 설정 실패:", error);
      toast.error("메인 그룹 설정 실패");
    }
  },

  addStock: async (token: JwtToken, groupId: string, stock: SearchResult) => {
    try {
      const res = await fetch(
        `/proxy/favorite/${token.memberId}/${groupId}?stockCode=${stock.stockCode}`,
        { method: "POST" }
      );

      if (!res.ok) {
        toast.error("종목 추가 실패");
        return;
      }

      toast.success(`${stock.stockName} 종목이 추가되었습니다.`);

      // 추가하려는 그룹이 현재 선택된 그룹과 같을 때만 전역 상태 업데이트
      const { selectedGroupId, interestStocks } = get();
      if (selectedGroupId === groupId) {
        set({
          interestStocks: [
            ...interestStocks,
            { stockInfo: stock, stockSequence: interestStocks.length + 1 },
          ],
        });
      }
    } catch (error) {
      console.error("종목 추가 실패:", error);
      toast.error("종목 추가 실패");
    }
  },

  deleteStock: async (token: JwtToken, groupId: string, stockCode: string) => {
    try {
      const res = await fetch(
        `/proxy/favorite/${token.memberId}/${groupId}/stock?stockCode=${stockCode}`,
        { method: "DELETE" }
      );

      if (!res.ok) {
        toast.error("종목 삭제 실패");
        return;
      }

      toast.success("종목이 삭제되었습니다.");

      // 삭제하려는 그룹이 현재 선택된 그룹과 같을 때만 전역 상태 업데이트
      const { selectedGroupId, interestStocks } = get();
      if (selectedGroupId === groupId) {
        set({
          interestStocks: interestStocks.filter(
            (stock) => stock.stockInfo.stockCode !== stockCode
          ),
        });
      }
    } catch (error) {
      console.error("종목 삭제 실패:", error);
      toast.error("종목 삭제 실패");
    }
  },
}));
