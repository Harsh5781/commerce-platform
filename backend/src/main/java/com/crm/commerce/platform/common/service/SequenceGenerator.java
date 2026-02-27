package com.crm.commerce.platform.common.service;

import org.springframework.stereotype.Service;

@Service
public interface SequenceGenerator {

    long nextValue(String sequenceName);
}
