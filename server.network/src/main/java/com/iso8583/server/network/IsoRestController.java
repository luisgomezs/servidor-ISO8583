package com.iso8583.server.network;

import com.iso8583.server.model.TransactionEntity;
import com.iso8583.server.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class IsoRestController {

    @Autowired
    private TransactionRepository transactionRepository;

    @GetMapping("/api/iso/test")
    public Map<String, Object> testTransaction(
            @RequestParam(defaultValue = "4111112222333344") String pan,
            @RequestParam(defaultValue = "POS_DEFAULT") String terminal,
            @RequestParam(defaultValue = "1000") Double amount) {

        String maskedPan = maskPan(pan);

        TransactionEntity tx = new TransactionEntity();
        tx.setTerminalId(terminal);
        tx.setMaskedPan(maskedPan);
        tx.setAmount(amount);
        tx.setResponseCode("00");
        transactionRepository.save(tx);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("message", "Transacción procesada y guardada en PostgreSQL con éxito");
        response.put("transactionId", tx.getId());
        response.put("terminalId", terminal);
        response.put("simulatedPanMasked", maskedPan);
        response.put("amount", amount);
        response.put("responseCode", "00");

        return response;
    }

    @GetMapping("/api/iso/transactions")
    public List<TransactionEntity> getAllTransactions() {
        return transactionRepository.findAll();
    }

    private String maskPan(String pan) {
        if (pan == null || pan.length() < 10) return "********";
        return pan.substring(0, 6) + "******" + pan.substring(pan.length() - 4);
    }
}