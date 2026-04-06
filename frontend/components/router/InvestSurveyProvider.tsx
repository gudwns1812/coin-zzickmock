"use client";

import Modal from "@/components/ui/Modal";
import { JwtToken } from "@/type/jwt";
import clsx from "clsx";
import { ChevronLeft, ChevronRight } from "lucide-react";
import React, { useEffect, useState } from "react";
import { toast } from "react-toastify";
import Button from "../ui/shared/Button";

const FormModal = Modal;

const InvestSurveyProvider = ({
  token,
  children,
}: {
  token: JwtToken | null;
  children: React.ReactNode;
}) => {
  const [questionPage, setQuestionPage] = useState(0);
  const [isAccept, setIsAccept] = useState(false);
  const [selectedAnswer, setSelectedAnswer] = useState<number[]>([
    0, 0, 0, 0, 0,
  ]);

  useEffect(() => {
    if (!token) return;
    if (token.invest === 0) {
      setQuestionPage(0);
      setIsAccept(false);
      setSelectedAnswer([0, 0, 0, 0, 0]);
    }
  }, [token]);

  const handleNextQuestion = () => {
    setQuestionPage(questionPage + 1);
  };

  const handlePrevQuestion = () => {
    setQuestionPage(questionPage - 1);
  };

  return (
    <>
      {children}
      <FormModal
        isOpen={!!token && token.invest === 0}
        onClose={() => {}}
        isClickOutsideClose={false}
        hasCloseButton={false}
      >
        <div className="flex flex-col gap-main-2 px-main w-[600px]">
          <h2 className="text-xl-custom font-bold text-main-dark-gray">
            투자성향 설정
          </h2>

          <div
            className={clsx(
              "w-full h-[5px] rounded-full bg-main-dark-gray/10",
              questionPage === 0 && "hidden"
            )}
          >
            <div
              className={clsx(
                "h-full bg-main-blue rounded-full transition-all duration-300",
                questionPage === 1 && "w-0",
                questionPage === 2 && "w-1/5",
                questionPage === 3 && "w-2/5",
                questionPage === 4 && "w-3/5",
                questionPage === 5 && "w-4/5"
              )}
            />
          </div>

          {questionPage === 0 && (
            <AcceptForm
              next={handleNextQuestion}
              setIsAccept={setIsAccept}
              isAccept={isAccept}
            />
          )}

          {questionPage === 1 && (
            <Question1
              selectedAnswer={selectedAnswer}
              setSelectedAnswer={setSelectedAnswer}
              prev={handlePrevQuestion}
              next={handleNextQuestion}
            />
          )}
          {questionPage === 2 && (
            <Question2
              selectedAnswer={selectedAnswer}
              setSelectedAnswer={setSelectedAnswer}
              prev={handlePrevQuestion}
              next={handleNextQuestion}
            />
          )}
          {questionPage === 3 && (
            <Question3
              selectedAnswer={selectedAnswer}
              setSelectedAnswer={setSelectedAnswer}
              prev={handlePrevQuestion}
              next={handleNextQuestion}
            />
          )}
          {questionPage === 4 && (
            <Question4
              selectedAnswer={selectedAnswer}
              setSelectedAnswer={setSelectedAnswer}
              prev={handlePrevQuestion}
              next={handleNextQuestion}
            />
          )}
          {questionPage === 5 && (
            <Question5
              selectedAnswer={selectedAnswer}
              setSelectedAnswer={setSelectedAnswer}
              prev={handlePrevQuestion}
              next={handleNextQuestion}
            />
          )}

          {questionPage === 6 && (
            <ResultForm
              onClose={() => {}}
              selectedAnswer={selectedAnswer}
              setInvestScore={() => {}}
              token={token}
            />
          )}
        </div>
      </FormModal>
    </>
  );
};

interface QuestionProps {
  prev: () => void;
  next: () => void;
  selectedAnswer: number[];
  setSelectedAnswer: (answer: number[]) => void;
}

const handleSelectAnswer = (
  questionIndex: number,
  answerIndex: number,
  selectedAnswer: number[],
  setSelectedAnswer: (answer: number[]) => void
) => {
  const copy = [...selectedAnswer];
  copy[questionIndex] = answerIndex;
  setSelectedAnswer(copy);
};

const AcceptForm = ({
  next,
  setIsAccept,
  isAccept,
}: {
  next: () => void;
  setIsAccept: (isAccept: boolean) => void;
  isAccept: boolean;
}) => {
  return (
    <div className="flex flex-col gap-main">
      <h2 className="text-lg-custom font-semibold">
        회원 서비스는 회원님의 투자자 성향이 필요해요
      </h2>

      <p className="text-sm-custom text-main-gray leading-8 my-main">
        금융투자상품의 적합성 판단을 위해 투자자 성향에 대한 진단이 필요합니다.
        <br />
        해당 설문은 고객님의 투자 목적, 경험, 위험 선호도를 기반으로
        성향을분석하며,
        <br />
        결과는 향후 상품 추천 및 자산 포트폴리오 구성에 활용됩니다.
        <br />
        성향 분석을 위해 입력하신 정보는 관련 법령에 따라 안전하게 처리됩니다.
      </p>

      <label className="flex items-center gap-2 text-sm-custom text-main-gray">
        <input
          type="checkbox"
          checked={isAccept}
          onChange={() => setIsAccept(!isAccept)}
        />
        <span className="text-sm-custom text-main-gray leading-relaxed">
          본인은 투자자 성향 진단을 위한 정보 수집 및 활용에 동의합니다.
        </span>
      </label>

      <div className="flex justify-end">
        <Button
          variant="ghost"
          className="w-fit"
          onClick={next}
          disabled={!isAccept}
        >
          동의
        </Button>
      </div>
    </div>
  );
};

const Question1 = ({
  next,
  selectedAnswer,
  setSelectedAnswer,
}: QuestionProps) => {
  return (
    <div className="flex flex-col gap-main">
      <h2 className="text-lg-custom font-semibold">
        Q1. 귀하의 주된 투자 목적은 무엇입니까?
      </h2>

      <div className="grid grid-rows-5 gap-main">
        <button
          className={clsx(
            "hover:text-main-blue hover:bg-main-blue/10 transition-all duration-300 rounded-main px-main py-1 flex items-center gap-1",
            selectedAnswer[0] === 1 && "text-main-blue bg-main-blue/10"
          )}
          onClick={() => {
            handleSelectAnswer(0, 1, selectedAnswer, setSelectedAnswer);
          }}
        >
          원금 손실이 없도록 안정적인 자산 운용
        </button>
        <button
          className={clsx(
            "hover:text-main-blue hover:bg-main-blue/10 transition-all duration-300 rounded-main px-main py-1 flex items-center gap-1",
            selectedAnswer[0] === 2 && "text-main-blue bg-main-blue/10"
          )}
          onClick={() => {
            handleSelectAnswer(0, 2, selectedAnswer, setSelectedAnswer);
          }}
        >
          물가상승률을 웃도는 수익
        </button>
        <button
          className={clsx(
            "hover:text-main-blue hover:bg-main-blue/10 transition-all duration-300 rounded-main px-main py-1 flex items-center gap-1",
            selectedAnswer[0] === 3 && "text-main-blue bg-main-blue/10"
          )}
          onClick={() => {
            handleSelectAnswer(0, 3, selectedAnswer, setSelectedAnswer);
          }}
        >
          자산을 증식하고자 하는 목적
        </button>
        <button
          className={clsx(
            "hover:text-main-blue hover:bg-main-blue/10 transition-all duration-300 rounded-main px-main py-1 flex items-center gap-1",
            selectedAnswer[0] === 4 && "text-main-blue bg-main-blue/10"
          )}
          onClick={() => {
            handleSelectAnswer(0, 4, selectedAnswer, setSelectedAnswer);
          }}
        >
          고수익을 위해 위험도 감수
        </button>
        <button
          className={clsx(
            "hover:text-main-blue hover:bg-main-blue/10 transition-all duration-300 rounded-main px-main py-1 flex items-center gap-1",
            selectedAnswer[0] === 5 && "text-main-blue bg-main-blue/10"
          )}
          onClick={() => {
            handleSelectAnswer(0, 5, selectedAnswer, setSelectedAnswer);
          }}
        >
          단기 차익 실현을 위한 공격적 투자
        </button>
      </div>

      <div className="flex justify-end">
        <Button
          variant="ghost"
          className="w-fit flex items-center gap-1"
          onClick={next}
          disabled={selectedAnswer[0] === 0}
        >
          <span>다음</span>
          <ChevronRight size={16} />
        </Button>
      </div>
    </div>
  );
};

const Question2 = ({
  prev,
  next,
  selectedAnswer,
  setSelectedAnswer,
}: QuestionProps) => {
  return (
    <div className="flex flex-col gap-main">
      <h2 className="text-lg-custom font-semibold">
        Q2. 금융상품(주식/채권 등) 투자 경험이 있으신가요?
      </h2>

      <div className="grid grid-rows-5 gap-main">
        <button
          className={clsx(
            "hover:text-main-blue hover:bg-main-blue/10 transition-all duration-300 rounded-main px-main py-1 flex items-center gap-1",
            selectedAnswer[1] === 1 && "text-main-blue bg-main-blue/10"
          )}
          onClick={() => {
            handleSelectAnswer(1, 1, selectedAnswer, setSelectedAnswer);
          }}
        >
          없음
        </button>
        <button
          className={clsx(
            "hover:text-main-blue hover:bg-main-blue/10 transition-all duration-300 rounded-main px-main py-1 flex items-center gap-1",
            selectedAnswer[1] === 2 && "text-main-blue bg-main-blue/10"
          )}
          onClick={() => {
            handleSelectAnswer(1, 2, selectedAnswer, setSelectedAnswer);
          }}
        >
          예/적금 외에 간접투자(펀드 등) 경험 있음
        </button>
        <button
          className={clsx(
            "hover:text-main-blue hover:bg-main-blue/10 transition-all duration-300 rounded-main px-main py-1 flex items-center gap-1",
            selectedAnswer[1] === 3 && "text-main-blue bg-main-blue/10"
          )}
          onClick={() => {
            handleSelectAnswer(1, 3, selectedAnswer, setSelectedAnswer);
          }}
        >
          직접투자(국내주식 등) 경험 있음
        </button>
        <button
          className={clsx(
            "hover:text-main-blue hover:bg-main-blue/10 transition-all duration-300 rounded-main px-main py-1 flex items-center gap-1",
            selectedAnswer[1] === 4 && "text-main-blue bg-main-blue/10"
          )}
          onClick={() => {
            handleSelectAnswer(1, 4, selectedAnswer, setSelectedAnswer);
          }}
        >
          직접투자(해외주식/ETF 등) 경험 있고 손실 경험도 있음
        </button>
        <button
          className={clsx(
            "hover:text-main-blue hover:bg-main-blue/10 transition-all duration-300 rounded-main px-main py-1 flex items-center gap-1",
            selectedAnswer[1] === 5 && "text-main-blue bg-main-blue/10"
          )}
          onClick={() => {
            handleSelectAnswer(1, 5, selectedAnswer, setSelectedAnswer);
          }}
        >
          파생상품/고위험 상품 경험 있음
        </button>
      </div>

      <div className="flex justify-end">
        <Button
          variant="ghost"
          className="w-fit flex items-center gap-1"
          onClick={prev}
        >
          <ChevronLeft size={16} />
          <span>이전</span>
        </Button>
        <Button
          variant="ghost"
          className="w-fit flex items-center gap-1"
          onClick={next}
          disabled={selectedAnswer[1] === 0}
        >
          <span>다음</span>
          <ChevronRight size={16} />
        </Button>
      </div>
    </div>
  );
};

const Question3 = ({
  prev,
  next,
  selectedAnswer,
  setSelectedAnswer,
}: QuestionProps) => {
  return (
    <div className="flex flex-col gap-main">
      <h2 className="text-lg-custom font-semibold">
        Q3. 1년 내 원금의 20% 손실 가능성이 있는 투자상품을 추천받는다면?
      </h2>

      <div className="grid grid-rows-5 gap-main">
        <button
          className={clsx(
            "hover:text-main-blue hover:bg-main-blue/10 transition-all duration-300 rounded-main px-main py-1 flex items-center gap-1",
            selectedAnswer[2] === 1 && "text-main-blue bg-main-blue/10"
          )}
          onClick={() => {
            handleSelectAnswer(2, 1, selectedAnswer, setSelectedAnswer);
          }}
        >
          절대 하지 않음
        </button>
        <button
          className={clsx(
            "hover:text-main-blue hover:bg-main-blue/10 transition-all duration-300 rounded-main px-main py-1 flex items-center gap-1",
            selectedAnswer[2] === 2 && "text-main-blue bg-main-blue/10"
          )}
          onClick={() => {
            handleSelectAnswer(2, 2, selectedAnswer, setSelectedAnswer);
          }}
        >
          신중히 고려하되 가능성 낮음
        </button>
        <button
          className={clsx(
            "hover:text-main-blue hover:bg-main-blue/10 transition-all duration-300 rounded-main px-main py-1 flex items-center gap-1",
            selectedAnswer[2] === 3 && "text-main-blue bg-main-blue/10"
          )}
          onClick={() => {
            handleSelectAnswer(2, 3, selectedAnswer, setSelectedAnswer);
          }}
        >
          일정 수익 기대된다면 고려함
        </button>
        <button
          className={clsx(
            "hover:text-main-blue hover:bg-main-blue/10 transition-all duration-300 rounded-main px-main py-1 flex items-center gap-1",
            selectedAnswer[2] === 4 && "text-main-blue bg-main-blue/10"
          )}
          onClick={() => {
            handleSelectAnswer(2, 4, selectedAnswer, setSelectedAnswer);
          }}
        >
          손실을 감수하고도 투자함
        </button>
        <button
          className={clsx(
            "hover:text-main-blue hover:bg-main-blue/10 transition-all duration-300 rounded-main px-main py-1 flex items-center gap-1",
            selectedAnswer[2] === 5 && "text-main-blue bg-main-blue/10"
          )}
          onClick={() => {
            handleSelectAnswer(2, 5, selectedAnswer, setSelectedAnswer);
          }}
        >
          적극적으로 투자하겠음
        </button>
      </div>

      <div className="flex justify-end">
        <Button
          variant="ghost"
          className="w-fit flex items-center gap-1"
          onClick={prev}
        >
          <ChevronLeft size={16} />
          <span>이전</span>
        </Button>
        <Button
          variant="ghost"
          className="w-fit flex items-center gap-1"
          onClick={next}
          disabled={selectedAnswer[2] === 0}
        >
          <span>다음</span>
          <ChevronRight size={16} />
        </Button>
      </div>
    </div>
  );
};

const Question4 = ({
  prev,
  next,
  selectedAnswer,
  setSelectedAnswer,
}: QuestionProps) => {
  return (
    <div className="flex flex-col gap-main">
      <h2 className="text-lg-custom font-semibold">
        Q4. 투자 가능한 기간은 어느 정도인가요?
      </h2>

      <div className="grid grid-rows-5 gap-main">
        <button
          className={clsx(
            "hover:text-main-blue hover:bg-main-blue/10 transition-all duration-300 rounded-main px-main py-1 flex items-center gap-1",
            selectedAnswer[3] === 1 && "text-main-blue bg-main-blue/10"
          )}
          onClick={() => {
            handleSelectAnswer(3, 1, selectedAnswer, setSelectedAnswer);
          }}
        >
          1년 이하
        </button>
        <button
          className={clsx(
            "hover:text-main-blue hover:bg-main-blue/10 transition-all duration-300 rounded-main px-main py-1 flex items-center gap-1",
            selectedAnswer[3] === 2 && "text-main-blue bg-main-blue/10"
          )}
          onClick={() => {
            handleSelectAnswer(3, 2, selectedAnswer, setSelectedAnswer);
          }}
        >
          1~3년
        </button>
        <button
          className={clsx(
            "hover:text-main-blue hover:bg-main-blue/10 transition-all duration-300 rounded-main px-main py-1 flex items-center gap-1",
            selectedAnswer[3] === 3 && "text-main-blue bg-main-blue/10"
          )}
          onClick={() => {
            handleSelectAnswer(3, 3, selectedAnswer, setSelectedAnswer);
          }}
        >
          3~5년
        </button>
        <button
          className={clsx(
            "hover:text-main-blue hover:bg-main-blue/10 transition-all duration-300 rounded-main px-main py-1 flex items-center gap-1",
            selectedAnswer[3] === 4 && "text-main-blue bg-main-blue/10"
          )}
          onClick={() => {
            handleSelectAnswer(3, 4, selectedAnswer, setSelectedAnswer);
          }}
        >
          5~10년
        </button>
        <button
          className={clsx(
            "hover:text-main-blue hover:bg-main-blue/10 transition-all duration-300 rounded-main px-main py-1 flex items-center gap-1",
            selectedAnswer[3] === 5 && "text-main-blue bg-main-blue/10"
          )}
          onClick={() => {
            handleSelectAnswer(3, 5, selectedAnswer, setSelectedAnswer);
          }}
        >
          10년 이상
        </button>
      </div>

      <div className="flex justify-end">
        <Button
          variant="ghost"
          className="w-fit flex items-center gap-1"
          onClick={prev}
        >
          <ChevronLeft size={16} />
          <span>이전</span>
        </Button>
        <Button
          variant="ghost"
          className="w-fit flex items-center gap-1"
          onClick={next}
          disabled={selectedAnswer[3] === 0}
        >
          <span>다음</span>
          <ChevronRight size={16} />
        </Button>
      </div>
    </div>
  );
};

const Question5 = ({
  prev,
  next,
  selectedAnswer,
  setSelectedAnswer,
}: QuestionProps) => {
  return (
    <div className="flex flex-col gap-main">
      <h2 className="text-lg-custom font-semibold">
        Q5. 투자 원금 대비 손실 가능성이 있을 때 본인의 반응은?
      </h2>

      <div className="grid grid-rows-5 gap-main">
        <button
          className={clsx(
            "hover:text-main-blue hover:bg-main-blue/10 transition-all duration-300 rounded-main px-main py-1 flex items-center gap-1",
            selectedAnswer[4] === 1 && "text-main-blue bg-main-blue/10"
          )}
          onClick={() => {
            handleSelectAnswer(4, 1, selectedAnswer, setSelectedAnswer);
          }}
        >
          손실이 나면 투자 중단함
        </button>
        <button
          className={clsx(
            "hover:text-main-blue hover:bg-main-blue/10 transition-all duration-300 rounded-main px-main py-1 flex items-center gap-1",
            selectedAnswer[4] === 2 && "text-main-blue bg-main-blue/10"
          )}
          onClick={() => {
            handleSelectAnswer(4, 2, selectedAnswer, setSelectedAnswer);
          }}
        >
          손실이 나면 투자 줄임
        </button>
        <button
          className={clsx(
            "hover:text-main-blue hover:bg-main-blue/10 transition-all duration-300 rounded-main px-main py-1 flex items-center gap-1",
            selectedAnswer[4] === 3 && "text-main-blue bg-main-blue/10"
          )}
          onClick={() => {
            handleSelectAnswer(4, 3, selectedAnswer, setSelectedAnswer);
          }}
        >
          손실이 나면 기다려봄
        </button>
        <button
          className={clsx(
            "hover:text-main-blue hover:bg-main-blue/10 transition-all duration-300 rounded-main px-main py-1 flex items-center gap-1",
            selectedAnswer[4] === 4 && "text-main-blue bg-main-blue/10"
          )}
          onClick={() => {
            handleSelectAnswer(4, 4, selectedAnswer, setSelectedAnswer);
          }}
        >
          손실이 나도 추가 매수 고려
        </button>
        <button
          className={clsx(
            "hover:text-main-blue hover:bg-main-blue/10 transition-all duration-300 rounded-main px-main py-1 flex items-center gap-1",
            selectedAnswer[4] === 5 && "text-main-blue bg-main-blue/10"
          )}
          onClick={() => {
            handleSelectAnswer(4, 5, selectedAnswer, setSelectedAnswer);
          }}
        >
          손실이 나도 투자 전략 변경해서 계속 투자
        </button>
      </div>

      <div className="flex justify-end">
        <Button
          variant="ghost"
          className="w-fit flex items-center gap-1"
          onClick={prev}
        >
          <ChevronLeft size={16} />
          <span>이전</span>
        </Button>
        <Button
          variant="ghost"
          className="w-fit flex items-center gap-1"
          onClick={next}
          disabled={selectedAnswer[4] === 0}
        >
          <span>다음</span>
          <ChevronRight size={16} />
        </Button>
      </div>
    </div>
  );
};

const ResultForm = ({
  onClose,
  selectedAnswer,
  setInvestScore,
  token,
}: {
  onClose: () => void;
  selectedAnswer: number[];
  setInvestScore: React.Dispatch<React.SetStateAction<number>>;
  token: JwtToken | null;
}) => {
  useEffect(() => {
    const rawScore = selectedAnswer.reduce((acc, curr) => acc + curr, 0);
    const convertedScore = Math.round(rawScore / 5);
    setInvestScore(convertedScore);
  }, [selectedAnswer, setInvestScore]);

  const getInvestmentResult = (score: number) => {
    if (score === 1) {
      return {
        type: "🟦 안전형",
        description: `
      당신은 자산의 안전을 최우선으로 생각하는 매우 신중한 투자자입니다.<br/>
      원금 손실에 대한 두려움이 크며, 투자보다는 저축이나 예금 같은 자산 보호 중심의 금융상품에 더 익숙한 편이에요.<br/>
      
      이런 투자자에게는 예금, 적금, 국공채, MMF 같은 <b>초저위험 상품</b>이 적합하며,<br/>
      수익보다는 자산의 <b>보전(stability)</b>이 핵심 목적입니다.<br/>
      
      시장 변동성이 큰 주식이나 해외투자는 부담스러울 수 있으므로,<br/>
      금융 상품 가입 전에는 충분한 정보를 검토하고, 전문가의 조언을 참고하는 것이 좋아요.<br/>
        `,
      };
    } else if (score === 2) {
      return {
        type: "🟩 안정추구형",
        description: `
      당신은 손실에 대한 걱정이 있지만, 일정 수준의 수익을 기대하는 투자자입니다.<br/>
      위험을 회피하려는 성향이 있지만, 자산을 방치하지 않고 적극적으로 운용하려는 태도를 가지고 있어요.<br/>
      
      추천되는 상품은 <b>채권형 펀드</b>, <b>혼합형 펀드</b>, <b>우량 배당주 ETF</b> 등입니다.<br/>
      이런 상품은 상대적으로 안정적이면서도 예금보다 높은 수익률을 기대할 수 있어요.<br/>
      
      주식 투자는 부담스럽지만, 소액으로 ETF를 분산 투자하는 것도 좋은 방법이에요.<br/>
      리스크 관리를 위한 분산 투자 전략이 매우 중요합니다.<br/>
        `,
      };
    } else if (score === 3) {
      return {
        type: "🟨 위험중립형",
        description: `
      당신은 수익과 안정성 사이에서 균형을 추구하는 투자자입니다.<br/>
      일정 부분의 손실은 감수할 수 있지만, 전반적으로 위험 대비 수익을 고려해 투자하는 경향이 있어요.<br/>
      
      추천되는 포트폴리오는 주식과 채권의 <b>50:50 혼합</b> 또는, 상황에 따라 비중을 조절하는 <b>TDF(Target Date Fund)</b>, <b>밸런스형 ETF</b>입니다.<br/>
      
      시장 상황에 따라 전략을 유연하게 조정할 수 있는 능력이 중요하며,<br/>
      자산 배분과 분기별 리밸런싱을 통해 장기적인 수익을 추구할 수 있어요.<br/>
      
      이 성향의 투자자는 종목 선정보다 <b>자산 배분 전략</b>이 핵심입니다.
        `,
      };
    } else if (score === 4) {
      return {
        type: "🟧 적극투자형",
        description: `
      당신은 수익에 대한 욕구가 강하며, 일정 수준의 리스크를 감수할 수 있는 투자자입니다.<br/>
      단기보다는 중장기적인 관점에서 자산을 적극적으로 운용하려는 경향이 강해요.<br/>
      
      추천되는 투자 전략은 <b>개별 성장주 투자</b>, <b>섹터 ETF</b>, <b>해외주식</b>, <b>테마형 펀드</b>입니다.<br/>
      특히 기술주, 헬스케어, ESG 관련 테마에 관심을 가져볼 수 있어요.<br/>
      
      높은 수익률을 추구하는 만큼, 투자 후 리스크 관리가 중요하고,<br/>
      <b>리밸런싱</b>, <b>뉴스 기반 모니터링</b>, <b>포트폴리오 다변화</b>를 통해 리스크를 최소화하는 것이 좋아요.<br/>
        `,
      };
    } else {
      return {
        type: "🟥 공격투자형",
        description: `
      당신은 높은 수익률을 위해 큰 손실도 감수할 수 있는 고위험 성향의 투자자입니다.<br/>
      시장 흐름을 빠르게 파악하고 기회를 놓치지 않으려는 성향이 강하며, 과감한 결정을 내리는 데 주저하지 않아요.<br/>
      
      추천되는 전략은 <b>개별 고성장주 투자</b>, <b>단기 매매(스윙/데이 트레이딩)</b>, <b>해외 레버리지 ETF</b>, <b>암호화폐 등 고위험 고수익 자산군</b>입니다.<br/>
      
      이 성향의 투자자는 <b>정보 습득 능력</b>, <b>시장 모니터링</b>, <b>자기만의 기준</b>이 중요하며,<br/>
      수익률 변동이 큰 만큼 <b>멘탈 관리와 손절 기준</b> 설정도 꼭 필요해요.<br/>
      
      높은 기대 수익을 추구하되, 리스크가 항상 동반된다는 점을 명확히 인식해야 해요.
        `,
      };
    }
  };

  const rawScore = selectedAnswer.reduce((acc, curr) => acc + curr, 0);
  const convertedScore = Math.round(rawScore / 5);
  const { type, description } = getInvestmentResult(convertedScore);

  const handleClose = async () => {
    if (!token) return;
    const rawScore = selectedAnswer.reduce((acc, curr) => acc + curr, 0);
    // 5~25점을 1~5점으로 변환 (총점을 5로 나누어 반올림)
    const score = Math.round(rawScore / 5);
    const res = await fetch(`/proxy/auth/invest`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        memberId: token.memberId,
        investScore: score,
      }),
      credentials: "include",
    });

    if (!res.ok) {
      toast.error("투자자 성향 분석 결과를 저장하는데 실패했습니다.");
    } else {
      toast.success("투자자 성향 분석 결과를 저장했습니다.");
      // 성공 시 페이지 새로고침하여 업데이트된 토큰 정보 반영
      setTimeout(() => {
        window.location.reload();
      }, 1000); // 토스트 메시지를 보여준 후 새로고침
    }
  };

  return (
    <div className="flex flex-col gap-main">
      <h2 className="text-lg-custom font-semibold">투자자 성향 분석 결과</h2>

      <span className="text-lg-custom font-semibold">{type}</span>
      <p
        className="text-sm-custom text-main-dark-gray leading-relaxed"
        dangerouslySetInnerHTML={{ __html: description }}
      />

      <div className="flex justify-end">
        <Button
          variant="primary"
          className="w-fit flex items-center gap-1"
          onClick={handleClose}
        >
          <span>확인</span>
        </Button>
      </div>
    </div>
  );
};

export default InvestSurveyProvider;
