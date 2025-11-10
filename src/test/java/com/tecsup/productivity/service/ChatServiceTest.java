package com.tecsup.productivity.service;

import com.tecsup.productivity.dto.request.ChatMessageRequest;
import com.tecsup.productivity.dto.response.ChatMessageResponse;
import com.tecsup.productivity.entity.ChatMessage;
import com.tecsup.productivity.entity.Event;
import com.tecsup.productivity.entity.Task;
import com.tecsup.productivity.entity.User;
import com.tecsup.productivity.repository.ChatMessageRepository;
import com.tecsup.productivity.repository.EventRepository;
import com.tecsup.productivity.repository.TaskRepository;
import com.tecsup.productivity.util.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private SecurityUtil securityUtil;

    @InjectMocks
    private ChatService chatService;

    private User mockUser;
    private Map<String, Object> preferences;

    @BeforeEach
    void setUp() {
        preferences = new HashMap<>();
        preferences.put("chatEnabled", true);
        preferences.put("darkMode", false);

        mockUser = User.builder()
                .id(1L)
                .email("test@tecsup.edu.pe")
                .name("Test User")
                .preferences(preferences)
                .build();
    }

    @Test
    @DisplayName("HU-10, CA-15: Chatbot responde con datos de tareas pendientes")
    void testChatbotRespondsWithTasksData() {
        // Arrange
        ChatMessageRequest request = new ChatMessageRequest("¿Cuáles son mis tareas pendientes?");

        when(securityUtil.getCurrentUser()).thenReturn(mockUser);
        when(taskRepository.countPendingTasksByUser(1L)).thenReturn(3L);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage msg = invocation.getArgument(0);
            msg.setId(1L);
            return msg;
        });

        // Act
        ChatMessageResponse response = chatService.sendMessage(request);

        // Assert
        assertNotNull(response);
        assertEquals("¿Cuáles son mis tareas pendientes?", response.getMensaje());
        assertTrue(response.getRespuesta().contains("3 tareas pendientes"));

        verify(taskRepository).countPendingTasksByUser(1L);
        verify(chatMessageRepository).save(any(ChatMessage.class));
    }

    @Test
    @DisplayName("CA-15: Chatbot responde con datos de eventos del día")
    void testChatbotRespondsWithEventsData() {
        // Arrange
        ChatMessageRequest request = new ChatMessageRequest("¿Qué eventos tengo hoy?");

        Event event1 = Event.builder().id(1L).titulo("Reunión").build();
        Event event2 = Event.builder().id(2L).titulo("Clase").build();

        when(securityUtil.getCurrentUser()).thenReturn(mockUser);
        when(eventRepository.findByUserIdAndFechaOrderByHoraAsc(eq(1L), any(LocalDate.class)))
                .thenReturn(Arrays.asList(event1, event2));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage msg = invocation.getArgument(0);
            msg.setId(1L);
            return msg;
        });

        // Act
        ChatMessageResponse response = chatService.sendMessage(request);

        // Assert
        assertNotNull(response);
        assertTrue(response.getRespuesta().contains("2 eventos"));
        verify(eventRepository).findByUserIdAndFechaOrderByHoraAsc(eq(1L), any(LocalDate.class));
    }

    @Test
    @DisplayName("CA-15: Chatbot responde cuando no hay tareas")
    void testChatbotRespondsWithNoTasks() {
        // Arrange
        ChatMessageRequest request = new ChatMessageRequest("tareas pendientes");

        when(securityUtil.getCurrentUser()).thenReturn(mockUser);
        when(taskRepository.countPendingTasksByUser(1L)).thenReturn(0L);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage msg = invocation.getArgument(0);
            msg.setId(1L);
            return msg;
        });

        // Act
        ChatMessageResponse response = chatService.sendMessage(request);

        // Assert
        assertNotNull(response);
        assertTrue(response.getRespuesta().contains("No tienes tareas pendientes"));
    }

    @Test
    @DisplayName("CA-15: Chatbot responde con detalles de tarea única")
    void testChatbotRespondsWithSingleTaskDetails() {
        // Arrange
        ChatMessageRequest request = new ChatMessageRequest("mis tareas");

        Task task = Task.builder()
                .id(1L)
                .titulo("Completar proyecto")
                .prioridad(Task.TaskPriority.ALTA)
                .build();

        when(securityUtil.getCurrentUser()).thenReturn(mockUser);
        when(taskRepository.countPendingTasksByUser(1L)).thenReturn(1L);
        when(taskRepository.findByUserIdAndCompletedOrderByPrioridadAscCreatedAtDesc(1L, false))
                .thenReturn(Arrays.asList(task));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage msg = invocation.getArgument(0);
            msg.setId(1L);
            return msg;
        });

        // Act
        ChatMessageResponse response = chatService.sendMessage(request);

        // Assert
        assertNotNull(response);
        assertTrue(response.getRespuesta().contains("Completar proyecto"));
        assertTrue(response.getRespuesta().contains("ALTA"));
    }

    @Test
    @DisplayName("HU-11, CA-16: Chatbot deshabilitado lanza excepción")
    void testChatbotDisabledThrowsException() {
        // Arrange
        preferences.put("chatEnabled", false);
        mockUser.setPreferences(preferences);

        ChatMessageRequest request = new ChatMessageRequest("Hola");

        when(securityUtil.getCurrentUser()).thenReturn(mockUser);

        // Act & Assert
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> chatService.sendMessage(request)
        );

        assertEquals("El chatbot está deshabilitado", exception.getMessage());
        verify(chatMessageRepository, never()).save(any());
    }

    @Test
    @DisplayName("CA-16: Verificar que chatEnabled se respeta")
    void testChatEnabledPreferenceIsRespected() {
        // Arrange
        preferences.put("chatEnabled", true);
        mockUser.setPreferences(preferences);

        ChatMessageRequest request = new ChatMessageRequest("Hola");

        when(securityUtil.getCurrentUser()).thenReturn(mockUser);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage msg = invocation.getArgument(0);
            msg.setId(1L);
            return msg;
        });

        // Act
        ChatMessageResponse response = chatService.sendMessage(request);

        // Assert
        assertNotNull(response);
        verify(chatMessageRepository).save(any(ChatMessage.class));
    }

    @Test
    @DisplayName("HU-10: Obtener historial de chat")
    void testGetChatHistory() {
        // Arrange
        ChatMessage msg1 = ChatMessage.builder()
                .id(1L)
                .user(mockUser)
                .mensaje("Hola")
                .respuesta("¡Hola Test User!")
                .build();

        ChatMessage msg2 = ChatMessage.builder()
                .id(2L)
                .user(mockUser)
                .mensaje("¿Mis tareas?")
                .respuesta("Tienes 5 tareas pendientes")
                .build();

        when(securityUtil.getCurrentUser()).thenReturn(mockUser);
        when(chatMessageRepository.findRecentMessagesByUser(eq(1L), any(PageRequest.class)))
                .thenReturn(Arrays.asList(msg1, msg2));

        // Act
        List<ChatMessageResponse> responses = chatService.getChatHistory(50);

        // Assert
        assertNotNull(responses);
        assertEquals(2, responses.size());
        assertEquals("Hola", responses.get(0).getMensaje());
        assertEquals("¿Mis tareas?", responses.get(1).getMensaje());

        verify(chatMessageRepository).findRecentMessagesByUser(eq(1L), any(PageRequest.class));
    }

    @Test
    @DisplayName("CA-15: Chatbot responde a saludo con nombre del usuario")
    void testChatbotGreetsWithUserName() {
        // Arrange
        ChatMessageRequest request = new ChatMessageRequest("Hola");

        when(securityUtil.getCurrentUser()).thenReturn(mockUser);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage msg = invocation.getArgument(0);
            msg.setId(1L);
            return msg;
        });

        // Act
        ChatMessageResponse response = chatService.sendMessage(request);

        // Assert
        assertNotNull(response);
        assertTrue(response.getRespuesta().contains("Test User"));
    }

    @Test
    @DisplayName("CA-15: Chatbot responde a ayuda con opciones")
    void testChatbotRespondsToHelp() {
        // Arrange
        ChatMessageRequest request = new ChatMessageRequest("ayuda");

        when(securityUtil.getCurrentUser()).thenReturn(mockUser);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage msg = invocation.getArgument(0);
            msg.setId(1L);
            return msg;
        });

        // Act
        ChatMessageResponse response = chatService.sendMessage(request);

        // Assert
        assertNotNull(response);
        assertTrue(response.getRespuesta().contains("tareas pendientes"));
        assertTrue(response.getRespuesta().contains("eventos de hoy"));
    }

    @Test
    @DisplayName("Chatbot guarda mensaje y respuesta en DB")
    void testChatbotSavesMessageAndResponse() {
        // Arrange
        ChatMessageRequest request = new ChatMessageRequest("Test mensaje");

        when(securityUtil.getCurrentUser()).thenReturn(mockUser);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage msg = invocation.getArgument(0);
            assertEquals("Test mensaje", msg.getMensaje());
            assertNotNull(msg.getRespuesta());
            assertEquals(mockUser, msg.getUser());
            msg.setId(1L);
            return msg;
        });

        // Act
        chatService.sendMessage(request);

        // Assert
        verify(chatMessageRepository).save(argThat(msg ->
                "Test mensaje".equals(msg.getMensaje()) &&
                        msg.getRespuesta() != null &&
                        msg.getUser().equals(mockUser)
        ));
    }
}