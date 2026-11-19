package com.inhouse.inhouse_manager.player;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlayerService {
    private final PlayerRepository playerRepository;

    @Transactional
    public Long register(String name, String riotId, Position mainPosition, Position subPosition) {
        if (playerRepository.existsByRiotId(riotId)){
            throw new IllegalArgumentException("이미 등록된 라이엇 ID입니다: " + riotId);
        }
        Player player = new Player(name, riotId, mainPosition, subPosition);
        return playerRepository.save(player).getId();
    }

    public List<Player> findAll(){
        return playerRepository.findAll();
    }

    public Player findById(Long id){
        return playerRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 플레이어입니다: " + id));
    }
}
