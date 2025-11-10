// ============================================
// HabitWithProgressResponse.java - HU-4, CA-07
// ============================================
package com.tecsup.productivity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HabitWithProgressResponse {
    private HabitResponse habit;
    private HabitLogResponse progressToday;
}
