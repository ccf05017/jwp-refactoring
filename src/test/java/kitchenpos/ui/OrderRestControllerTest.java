package kitchenpos.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import kitchenpos.application.OrderService;
import kitchenpos.domain.Order;
import kitchenpos.domain.OrderStatus;
import kitchenpos.ui.dto.order.OrderResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class OrderRestControllerTest {
    private OrderRestController orderRestController;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private OrderService orderService;

    @BeforeEach
    void setup() {
        orderRestController = new OrderRestController(orderService);

        mockMvc = MockMvcBuilders.standaloneSetup(orderRestController).build();

        objectMapper = new ObjectMapper();
    }

    @DisplayName("주문을 생성할 수 있다.")
    @Test
    void createOrderTest() throws Exception {
        // given
        Long orderId = 1L;
        String url = "/api/orders";
        Order orderRequest = new Order();
        OrderResponse orderResponse = new OrderResponse(orderId, 1L, OrderStatus.MEAL.name(),
                LocalDateTime.now(), new ArrayList<>());

        given(orderService.create(any())).willReturn(orderResponse);

        // when, then
        mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", url + "/" + orderResponse.getId()))
                ;
    }

    @DisplayName("주문 목록을 조회할 수 있다.")
    @Test
    void getOrdersTest() throws Exception {
        // given
        String url = "/api/orders";
        OrderResponse orderResponse1 = new OrderResponse(1L, 1L, OrderStatus.MEAL.name(),
                LocalDateTime.now(), new ArrayList<>());
        OrderResponse orderResponse2 = new OrderResponse(2L, 1L, OrderStatus.MEAL.name(),
                LocalDateTime.now(), new ArrayList<>());

        given(orderService.list()).willReturn(Arrays.asList(orderResponse1, orderResponse2));

        // when, then
        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
        ;
    }

    @DisplayName("주문 상태를 바꿀 수 있다.")
    @Test
    void changeOrderTest() throws Exception {
        // given
        Long targetId = 1L;
        String url = "/api/orders/"+ targetId +"/order-status";

        Order changeOrderRequest = new Order();
        changeOrderRequest.changeOrderStatus(OrderStatus.MEAL.name());

        Order changedOrder = new Order();
        changedOrder.setId(targetId);
        changedOrder.changeOrderStatus(OrderStatus.MEAL.name());

        given(orderService.changeOrderStatus(eq(targetId), any())).willReturn(changedOrder);

        // when, then
        mockMvc.perform(put(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(changeOrderRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderStatus", is(OrderStatus.MEAL.name())))
                .andExpect(jsonPath("$.id", is(1)))
        ;
    }
}