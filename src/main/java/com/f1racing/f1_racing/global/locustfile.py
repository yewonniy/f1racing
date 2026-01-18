from locust import HttpUser, task, between
import random

# 테스트할 세션 키와 시간 범위
SESSIONS = {
    9472: {"year":2024, "start":1709392800000 , "end": 1709392830000},
    9480: {"year":2024, "start":1710004800000 , "end": 1710004830000}, 
    9488: {"year":2024, "start":1711251000000 , "end": 1711251030000}, 
    9496: {"year":2024, "start":1712467200000 , "end": 1712467230000}, 
    9673: {"year":2024, "start":1713684000000 , "end": 1713684030000}, 
    9507: {"year":2024, "start": 1714940400000, "end": 1714940430000}, 
    9515: {"year":2024, "start": 1747660800000, "end": 1747660830000}, 
    9523: {"year":2024, "start":1716729600000 , "end": 1716729630000}, 
    9531: {"year":2024, "start": 1717957200000, "end": 1717957230000}, 
    9539: {"year":2024, "start": 1719148800000, "end": 1719148830000}, 
    9550: {"year":2024, "start": 1719753600000, "end": 1719753630000}, 
    9558: {"year":2024, "start": 1720362000000, "end": 1720362030000}, 
    9566: {"year":2024, "start": 1721568000000, "end": 1721568030000}, 
    9574: {"year":2024, "start": 1722172800000, "end": 1722172830000}, 
    9582: {"year":2024, "start": 1724592000000, "end": 1724592030000}, 
    9590: {"year":2024, "start": 1725196800000, "end": 1725196830000}, 
    9598: {"year":2024, "start": 1726399200000, "end": 1726399230000}, 
    9606: {"year":2024, "start": 1727007600000, "end": 1727007630000}, 
    9617: {"year":2024, "start": 1729452000000, "end": 1729452030000}, 
    9625: {"year":2024, "start": 1730060400000, "end": 1730060430000}
    }
# START_TIME = 1710004800000
# END_TIME =   1710004830000 # 테스트용으로 앞부분 10초만 랜덤 조회

class F1RacingUser(HttpUser): # 이 클래스 1개 = 접속자 1명
    # 사용자가 슬라이더를 움직이는 간격 (1초 ~ 3초 사이 랜덤)
    # wait_time = between(1, 3)

    @task  # locust는 @task가 붙은 함수를 반복 실행함.
    def get_race_data(self):
        # 1. 준비된 세션 목록 중 하나를 랜덤 선택
        session_key = random.choice(list(SESSIONS.keys()))
        session_info = SESSIONS[session_key]
        
        # 2. 해당 세션의 유효한 시간 범위 내에서 랜덤 타임스탬프 생성
        # (시작 시간 ~ 종료 시간 사이의 랜덤 값)
        random_timestamp = random.randint(session_info["start"], session_info["end"])
        
        # 3. API 호출
        self.client.get(
            f"/api/race/skip/{session_key}",
            params={
                "timestamp": random_timestamp,
                "windowSize": 1000,
                "year": session_info["year"] # 연도도 세션에 맞게 동적으로!
            },
            # 리포트에는 세션 ID가 달라도 하나로 뭉쳐서 보이게 설정 (URL 패턴 그룹화)
            name="/api/race/skip/{sessionKey}" 
        )