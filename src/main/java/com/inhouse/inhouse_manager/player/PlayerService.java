package com.inhouse.inhouse_manager.player;

import com.inhouse.inhouse_manager.common.NotFoundException;
import com.inhouse.inhouse_manager.match.MatchParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlayerService {
    private final PlayerRepository playerRepository;
    private final MatchParticipantRepository matchParticipantRepository;

    @Transactional
    public Long register(String name, String riotId, Position mainPosition, Position subPosition) {
        String trimmedRiotId = riotId.strip();
        if (playerRepository.existsByRiotId(trimmedRiotId)){
            throw new IllegalArgumentException("이미 등록된 라이엇 ID입니다: " + trimmedRiotId);
        }
        Player player = new Player(name.strip(), trimmedRiotId, mainPosition, subPosition);
        return playerRepository.save(player).getId();
    }

    @Transactional
    public void update(Long id, String name, String riotId, Position mainPosition, Position subPosition) {
        Player player = findById(id);
        String trimmedRiotId = riotId.strip();
        if (playerRepository.existsByRiotIdAndIdNot(trimmedRiotId, id)) {
            throw new IllegalArgumentException("이미 등록된 라이엇 ID입니다: " + trimmedRiotId);
        }
        // 트랜잭션이 끝날 때 JPA가 바뀐 필드를 찾아 UPDATE 한다 (변경 감지). save()를 다시 부를 필요가 없다.
        player.update(name.strip(), trimmedRiotId, mainPosition, subPosition);
    }

    @Transactional
    public void delete(Long id) {
        Player player = findById(id);
        if (matchParticipantRepository.existsByPlayerId(id)) {
            throw new IllegalStateException("경기 기록이 있는 플레이어는 삭제할 수 없습니다.");
        }
        playerRepository.delete(player);
    }

    public List<Player> findAll(){
        return playerRepository.findAll();
    }

    public List<Player> findAllOrderByName() {
        return playerRepository.findAllByOrderByNameAsc();
    }

    public Player findById(Long id){
        return playerRepository.findById(id).orElseThrow(() -> new NotFoundException("존재하지 않는 플레이어입니다: " + id));
    }
}
