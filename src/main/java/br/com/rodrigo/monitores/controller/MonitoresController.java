package br.com.rodrigo.monitores.controller;

import br.com.rodrigo.monitores.dto.*;
import br.com.rodrigo.monitores.model.*;
import br.com.rodrigo.monitores.service.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
public class MonitoresController {

    @Autowired
    private MonitoresService monitoresService;

    @GetMapping("/alocacao")
    public ResponseEntity<List<AlocacaoDTO>> distribuir(@RequestParam Boolean respeitarGrupoCandidato) {
        List<AlocacaoDTO> alocacoes = monitoresService.distribuir(respeitarGrupoCandidato == null ? false : respeitarGrupoCandidato);
        return ResponseEntity.ok().body(alocacoes);
    }
}
