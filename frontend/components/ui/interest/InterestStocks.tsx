"use client";

import { JwtToken } from "@/type/jwt";
import React, { useEffect, useRef, useState } from "react";
import Dropdown from "../shared/Dropdown";
import {
  ChevronRight,
  Pencil,
  Plus,
  Settings2,
  Star,
  StarIcon,
  Trash2,
} from "lucide-react";
import Modal from "../Modal";
import SearchStock from "../SearchStock";

import clsx from "clsx";
import { toast } from "react-toastify";
import UpPrice from "../shared/UpPrice";
import DownPrice from "../shared/DownPrice";
// import Star from "@/components/lottie/star/Star";
import Image from "next/image";
import {
  useInterestStore,
  InterestGroup,
  InterestStock,
  SearchResult,
} from "@/store/useInterestStore";
import { IconButton } from "@/components/animate-ui/buttons/icon";
import Link from "next/link";

const SettingModal = Modal;

const InterestStocks = ({ token }: { token: JwtToken | null }) => {
  const [isOpenSettingModal, setIsOpenSettingModal] = useState(false);
  const [editingGroupId, setEditingGroupId] = useState<string | null>(null);
  const [editedGroupName, setEditedGroupName] = useState<string>("");
  const [modalSelectedGroupId, setModalSelectedGroupId] = useState<
    string | null
  >(null);
  const [modalStocks, setModalStocks] = useState<InterestStock[]>([]);
  const [updatingGroupId, setUpdatingGroupId] = useState<string | null>(null); // 중복 수정 방지
  const [settingMainGroupId, setSettingMainGroupId] = useState<string | null>(
    null
  ); // 메인 그룹 설정 중 방지

  const inputRefs = useRef<{ [id: string]: HTMLInputElement | null }>({});

  // 전역 스토어 사용
  const {
    interestGroups,
    selectedGroupId,
    interestStocks,
    isLoading,
    fetchGroups,
    fetchStocks,
    addGroup,
    updateGroupName,
    deleteGroup,
    setMainGroup,
    addStock,
    deleteStock,
    setSelectedGroupId,
  } = useInterestStore();

  // 초기 데이터 로드
  useEffect(() => {
    if (!token) return;
    fetchGroups(token);
  }, [token]); // eslint-disable-line react-hooks/exhaustive-deps

  // 선택된 그룹이 변경되면 종목 로드 (사이드바용)
  useEffect(() => {
    if (!token || !selectedGroupId) return;
    fetchStocks(token, selectedGroupId);
  }, [token, selectedGroupId]); // eslint-disable-line react-hooks/exhaustive-deps

  // 모달이 열릴 때 현재 선택된 그룹을 모달 로컬 상태로 설정
  useEffect(() => {
    if (isOpenSettingModal && selectedGroupId) {
      setModalSelectedGroupId(selectedGroupId);
    }
  }, [isOpenSettingModal, selectedGroupId]);

  // 모달에서 선택된 그룹이 변경되면 해당 그룹의 종목 데이터 가져오기
  useEffect(() => {
    if (!token || !modalSelectedGroupId || !isOpenSettingModal) return;

    if (modalSelectedGroupId === selectedGroupId) {
      // 현재 사이드바 그룹과 같으면 전역 상태 사용
      setModalStocks(interestStocks);
    } else {
      // 다른 그룹이면 별도로 API 호출하여 모달 전용 데이터 가져오기
      const fetchModalGroupStocks = async () => {
        try {
          const res = await fetch(
            `/proxy/favorite/${token.memberId}/${modalSelectedGroupId}`
          );
          const data: InterestStock[] = await res.json();

          if (res.ok) {
            setModalStocks(data);
          } else {
            setModalStocks([]);
          }
        } catch (error) {
          console.error("모달 그룹 종목 조회 실패:", error);
          setModalStocks([]);
        }
      };

      fetchModalGroupStocks();
    }
  }, [token, modalSelectedGroupId, isOpenSettingModal, selectedGroupId]);

  // 사이드바에서 선택된 그룹과 모달에서 선택된 그룹이 같을 때 데이터 동기화
  useEffect(() => {
    if (modalSelectedGroupId === selectedGroupId && isOpenSettingModal) {
      setModalStocks(interestStocks);
    }
  }, [
    modalSelectedGroupId,
    selectedGroupId,
    interestStocks,
    isOpenSettingModal,
  ]);

  const handleEditGroupName = (groupId: string, currentName: string) => {
    setEditingGroupId(groupId);
    setEditedGroupName(currentName);

    // 포커스 약간의 딜레이 주고 넣어줘야 작동 확실
    setTimeout(() => {
      inputRefs.current[groupId]?.focus();
    }, 0);
  };

  const handleEditGroupNameBlur = async (groupId: string) => {
    if (!token || updatingGroupId === groupId) return; // 이미 수정 중이면 중단

    setUpdatingGroupId(groupId); // 수정 시작

    try {
      await updateGroupName(token, groupId, editedGroupName);
    } finally {
      setEditingGroupId(null);
      setUpdatingGroupId(null); // 수정 완료
    }
  };

  const handleDeleteGroup = async (groupName: string, groupId: string) => {
    if (!token) return;
    await deleteGroup(token, groupId, groupName);
  };

  const handleAddGroup = async () => {
    if (!token) return;
    await addGroup(token);
  };

  const handleSetMainGroup = async (groupId: string) => {
    if (!token || settingMainGroupId) return;

    setSettingMainGroupId(groupId);
    try {
      await setMainGroup(token, groupId);
    } finally {
      setSettingMainGroupId(null);
    }
  };

  const handleAddStock = async (stock: SearchResult) => {
    if (!token || !modalSelectedGroupId) return;

    try {
      // 전역 스토어의 addStock 함수 사용
      await addStock(token, modalSelectedGroupId, stock);

      // 모달 그룹이 사이드바 그룹과 다를 때만 모달 로컬 상태 직접 업데이트
      // (같을 때는 useEffect가 자동으로 동기화함)
      if (modalSelectedGroupId !== selectedGroupId) {
        setModalStocks((prev) => [
          ...prev,
          { stockInfo: stock, stockSequence: prev.length + 1 },
        ]);
      }
    } catch (error) {
      console.error("종목 추가 실패:", error);
      toast.error("종목 추가 실패");
    }
  };

  const handleDeleteStock = async (stockCode: string) => {
    if (!token || !modalSelectedGroupId) return;

    try {
      // 전역 스토어의 deleteStock 함수 사용
      await deleteStock(token, modalSelectedGroupId, stockCode);

      // 모달 그룹이 사이드바 그룹과 다를 때만 모달 로컬 상태 직접 업데이트
      // (같을 때는 useEffect가 자동으로 동기화함)
      if (modalSelectedGroupId !== selectedGroupId) {
        setModalStocks((prev) =>
          prev.filter((stock) => stock.stockInfo.stockCode !== stockCode)
        );
      }
    } catch (error) {
      console.error("종목 삭제 실패:", error);
      toast.error("종목 삭제 실패");
    }
  };

  return (
    <>
      <div className="flex flex-col gap-main">
        <h2 className="text-xl-custom font-bold text-main-dark-gray">
          관심 종목
        </h2>

        {!token ? (
          <div className="w-full h-[120px] flex items-center justify-center text-main-dark-gray">
            로그인 후 이용해주세요
          </div>
        ) : (
          <>
            <div className="flex items-center justify-between">
              {interestGroups.length > 0 ? (
                <Dropdown
                  groups={interestGroups.map((group) => group.groupName)}
                  selected={
                    interestGroups.find(
                      (group) => group.groupId === selectedGroupId
                    )?.groupName || ""
                  }
                  onSelect={(groupName) => {
                    const foundGroup = interestGroups.find(
                      (group) => group.groupName === groupName
                    );
                    if (foundGroup) {
                      setSelectedGroupId(foundGroup.groupId);
                    }
                  }}
                  key={selectedGroupId}
                />
              ) : (
                <button
                  className="flex items-center gap-[5px] cursor-pointer text-main-dark-gray hover:bg-main-blue/10 rounded-main transition-colors duration-200 ease-in-out px-main py-1"
                  onClick={() => setIsOpenSettingModal(true)}
                >
                  <Plus size={16} />
                  <span>그룹추가</span>
                </button>
              )}

              <Settings2
                onClick={() => setIsOpenSettingModal(true)}
                className="text-main-dark-gray cursor-pointer p-2 box-content hover:bg-main-blue/20 rounded-full transition-colors duration-200 ease-in-out"
                size={20}
              />
            </div>

            <div className="flex-1 overflow-y-scroll">
              {interestStocks.length > 0 ? (
                <div className="flex flex-col">
                  {interestStocks.map((stock) => (
                    <Link
                      key={`sidebar-${selectedGroupId}-${stock.stockInfo.stockCode}`}
                      href={`/stock/${stock.stockInfo.stockCode}`}
                      className="flex gap-main items-center w-full rounded-main transition-colors duration-200 ease-in-out p-main cursor-pointer hover:bg-main-blue/10 group relative"
                    >
                      <ChevronRight
                        size={16}
                        className="absolute text-main-blue right-main top-1/2 -translate-y-1/2 opacity-0 group-hover:opacity-100 transition-opacity duration-200 ease-in-out"
                      />
                      <div className="relative flex items-center justify-center size-[40px] shrink-0">
                        {stock.stockInfo.stockImage ? (
                          <Image
                            src={stock.stockInfo.stockImage}
                            alt={stock.stockInfo.stockName}
                            fill
                            className="rounded-full"
                            sizes="40px"
                          />
                        ) : (
                          <div className="bg-main-blue/10 rounded-full size-[40px] shrink-0 flex items-center justify-center">
                            <span className="text-main-blue font-semibold">
                              {stock.stockInfo.stockName[0]}
                            </span>
                          </div>
                        )}
                      </div>
                      <div className="flex flex-col flex-1 truncate">
                        <div className="text-gray-800 truncate w-full flex items-baseline gap-1">
                          <span className="font-bold">
                            {stock.stockInfo.stockName}
                          </span>
                          <span className="text-gray-500 text-xs-custom">
                            {stock.stockInfo.stockCode}
                          </span>
                        </div>
                        <div className="text-sm-custom flex gap-main items-center">
                          <span
                            className={clsx(
                              "text-gray-500 text-sm-custom font-semibold",
                              (stock.stockInfo.sign === "1" ||
                                stock.stockInfo.sign === "2") &&
                                "text-main-red",
                              (stock.stockInfo.sign === "4" ||
                                stock.stockInfo.sign === "5") &&
                                "text-main-blue",
                              stock.stockInfo.sign === "3" && "text-gray-500"
                            )}
                          >
                            {Number(
                              stock.stockInfo.currentPrice
                            ).toLocaleString()}
                          </span>

                          <div className="flex justify-between h-fit">
                            {(stock.stockInfo.sign === "1" ||
                              stock.stockInfo.sign === "2") && (
                              <UpPrice
                                change={Number(stock.stockInfo.changeAmount)}
                                changeRate={Number(stock.stockInfo.changeRate)}
                              />
                            )}
                            {stock.stockInfo.sign === "3" && (
                              <span className="text-gray-400 font-medium">
                                {Number(stock.stockInfo.changeAmount)} (
                                {Number(stock.stockInfo.changeRate)}
                                %)
                              </span>
                            )}
                            {(stock.stockInfo.sign === "4" ||
                              stock.stockInfo.sign === "5") && (
                              <DownPrice
                                change={Number(stock.stockInfo.changeAmount)}
                                changeRate={Number(stock.stockInfo.changeRate)}
                              />
                            )}
                          </div>
                        </div>
                      </div>
                    </Link>
                  ))}
                </div>
              ) : (
                <div className="w-full h-[120px] flex items-center justify-center text-main-dark-gray flex-1">
                  관심 종목이 없습니다.
                </div>
              )}
            </div>
          </>
        )}
      </div>

      {token && (
        <SettingModal
          isOpen={isOpenSettingModal}
          onClose={() => {
            setIsOpenSettingModal(false);
            setModalSelectedGroupId(null);
            setModalStocks([]);
          }}
        >
          <div className="flex flex-col gap-main">
            <h2 className="text-xl-custom font-bold text-main-dark-gray">
              관심 종목 설정
            </h2>

            <h4 className="text-sm-custom text-main-dark-gray flex items-center justify-between">
              <span>그룹과 종목을 관리할 수 있습니다.</span>

              {interestGroups.length > 0 && modalSelectedGroupId && (
                <SearchStock
                  onSelect={(stock) => {
                    handleAddStock(stock);
                  }}
                />
              )}
            </h4>

            <div className="grid grid-cols-[350px_1px_350px] gap-main h-[500px]">
              <div className="flex flex-col gap-main">
                <div className="flex-1 flex flex-col gap-main overflow-y-scroll">
                  {interestGroups.length === 0 && (
                    <div className="flex items-center justify-center h-full">
                      <span className="text-main-dark-gray">
                        그룹을 생성해주세요.
                      </span>
                    </div>
                  )}

                  {interestGroups.length > 0 &&
                    interestGroups.map((group) => (
                      <div
                        key={group.groupId}
                        className={clsx(
                          "flex items-center justify-between hover:bg-main-blue/10 rounded-main transition-colors duration-200 ease-in-out p-main cursor-pointer group",
                          modalSelectedGroupId === group.groupId &&
                            "bg-main-blue/10"
                        )}
                        onClick={() => setModalSelectedGroupId(group.groupId)}
                      >
                        <div className="flex items-center gap-main">
                          <IconButton
                            icon={Star}
                            active={group.main}
                            disabled={settingMainGroupId !== null}
                            onClick={(e) => {
                              e.stopPropagation();
                              handleSetMainGroup(group.groupId);
                            }}
                          />

                          <input
                            ref={(el) => {
                              if (el) {
                                inputRefs.current[group.groupId] = el;
                              }
                            }}
                            maxLength={10}
                            className={clsx(
                              "border-none bg-transparent outline-none",
                              editingGroupId !== group.groupId &&
                                "pointer-events-none"
                            )}
                            readOnly={editingGroupId !== group.groupId}
                            value={
                              editingGroupId === group.groupId
                                ? editedGroupName
                                : group.groupName
                            }
                            onChange={(e) => setEditedGroupName(e.target.value)}
                            onBlur={() => {
                              if (editingGroupId === group.groupId) {
                                handleEditGroupNameBlur(group.groupId);
                              }
                            }}
                            onKeyDown={(e) => {
                              if (e.key === "Enter") {
                                inputRefs.current[group.groupId]?.blur();
                              }
                            }}
                          />
                        </div>

                        <div className="flex items-center gap-1">
                          <button
                            className="text-main-dark-gray opacity-0 group-hover:opacity-100 transition-opacity duration-200 ease-in-out hover:text-main-blue hover:bg-main-blue/10 rounded-full p-1 disabled:opacity-50 disabled:cursor-not-allowed"
                            disabled={settingMainGroupId !== null}
                            onClick={(e) => {
                              e.stopPropagation();
                              handleEditGroupName(
                                group.groupId,
                                group.groupName
                              );
                            }}
                          >
                            <Pencil size={14} />
                          </button>

                          {/* 메인 그룹이 아니고, 전체 그룹이 2개 이상일 때만 삭제 버튼 표시 */}
                          {!group.main && interestGroups.length > 1 && (
                            <button
                              className="text-main-dark-gray opacity-0 group-hover:opacity-100 transition-opacity duration-200 ease-in-out hover:text-main-blue hover:bg-main-blue/10 rounded-full p-1 disabled:opacity-50 disabled:cursor-not-allowed"
                              disabled={settingMainGroupId !== null}
                              onClick={(e) => {
                                e.stopPropagation();
                                handleDeleteGroup(
                                  group.groupName,
                                  group.groupId
                                );
                              }}
                            >
                              <Trash2 size={14} />
                            </button>
                          )}
                        </div>
                      </div>
                    ))}
                </div>

                <button
                  className="w-full flex items-center gap-main justify-center text-white bg-main-blue rounded-main transition-colors duration-200 ease-in-out p-main"
                  onClick={handleAddGroup}
                >
                  <Plus size={20} />
                  <span>그룹추가</span>
                </button>
              </div>

              <div className="h-full bg-main-dark-gray/10" />

              <div className="flex flex-col gap-main h-full overflow-y-scroll">
                {modalSelectedGroupId && (
                  <div className="flex flex-col gap-main">
                    {modalStocks.length > 0 ? (
                      <div>
                        {modalStocks.map((stock) => (
                          <div
                            key={`modal-${modalSelectedGroupId}-${stock.stockInfo.stockCode}`}
                            className="flex gap-main items-center w-full rounded-main transition-colors duration-200 ease-in-out p-main hover:bg-main-blue/10 group"
                          >
                            <div className="relative flex items-center justify-center size-[40px] shrink-0">
                              {stock.stockInfo.stockImage ? (
                                <Image
                                  src={stock.stockInfo.stockImage}
                                  alt={stock.stockInfo.stockName}
                                  fill
                                  className="rounded-full"
                                  sizes="40px"
                                />
                              ) : (
                                <div className="bg-main-blue/10 rounded-full size-[40px] shrink-0 flex items-center justify-center">
                                  <span className="text-main-blue font-semibold">
                                    {stock.stockInfo.stockName[0]}
                                  </span>
                                </div>
                              )}
                            </div>
                            <div className="flex flex-col flex-1 truncate">
                              <div className="text-gray-800 truncate w-full flex items-baseline gap-1">
                                <span className="font-bold">
                                  {stock.stockInfo.stockName}
                                </span>
                                <span className="text-gray-500 text-xs-custom">
                                  {stock.stockInfo.stockCode}
                                </span>
                              </div>
                              <div className="text-sm-custom flex gap-main items-center">
                                <span
                                  className={clsx(
                                    "text-gray-500 text-sm-custom font-semibold",
                                    (stock.stockInfo.sign === "1" ||
                                      stock.stockInfo.sign === "2") &&
                                      "text-main-red",
                                    (stock.stockInfo.sign === "4" ||
                                      stock.stockInfo.sign === "5") &&
                                      "text-main-blue",
                                    stock.stockInfo.sign === "3" &&
                                      "text-gray-500"
                                  )}
                                >
                                  {Number(
                                    stock.stockInfo.currentPrice
                                  ).toLocaleString()}
                                </span>

                                <div className="flex justify-between h-fit">
                                  {(stock.stockInfo.sign === "1" ||
                                    stock.stockInfo.sign === "2") && (
                                    <UpPrice
                                      change={Number(
                                        stock.stockInfo.changeAmount
                                      )}
                                      changeRate={Number(
                                        stock.stockInfo.changeRate
                                      )}
                                    />
                                  )}
                                  {stock.stockInfo.sign === "3" && (
                                    <span className="text-gray-400 font-medium">
                                      {Number(stock.stockInfo.changeAmount)} (
                                      {Number(stock.stockInfo.changeRate)}
                                      %)
                                    </span>
                                  )}
                                  {(stock.stockInfo.sign === "4" ||
                                    stock.stockInfo.sign === "5") && (
                                    <DownPrice
                                      change={Number(
                                        stock.stockInfo.changeAmount
                                      )}
                                      changeRate={Number(
                                        stock.stockInfo.changeRate
                                      )}
                                    />
                                  )}
                                </div>
                              </div>
                            </div>

                            <button
                              className="text-main-dark-gray opacity-0 group-hover:opacity-100 transition-opacity duration-200 ease-in-out hover:text-main-blue hover:bg-main-blue/10 rounded-full p-1"
                              onClick={(e) => {
                                e.stopPropagation();
                                handleDeleteStock(stock.stockInfo.stockCode);
                              }}
                            >
                              <Trash2 size={16} />
                            </button>
                          </div>
                        ))}
                      </div>
                    ) : (
                      <div className="flex items-center justify-center h-full">
                        <span className="text-main-dark-gray">
                          종목을 추가해주세요.
                        </span>
                      </div>
                    )}
                  </div>
                )}
              </div>
            </div>
          </div>
        </SettingModal>
      )}
    </>
  );
};

export default InterestStocks;
