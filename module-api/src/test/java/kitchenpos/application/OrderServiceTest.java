package kitchenpos.application;

import dto.order.OrderLineItemRequest;
import dto.order.OrderRequest;
import dto.order.OrderResponse;
import dto.order.OrderStatusChangeRequest;
import dto.ordertable.ChangeEmptyRequest;
import dto.ordertable.OrderTableRequest;
import dto.ordertable.OrderTableResponse;
import kitchenpos.OrderService;
import kitchenpos.OrderTableService;
import kitchenpos.domain.menu.Menu;
import kitchenpos.domain.menu.MenuProduct;
import kitchenpos.domain.menu.MenuRepository;
import kitchenpos.domain.menugroup.MenuGroup;
import kitchenpos.domain.menugroup.MenuGroupRepository;
import kitchenpos.domain.order.OrderStatus;
import kitchenpos.domain.order.exceptions.InvalidTryChangeOrderStatusException;
import kitchenpos.domain.order.exceptions.InvalidTryOrderException;
import kitchenpos.domain.order.exceptions.MenuEntityNotFoundException;
import kitchenpos.domain.order.exceptions.OrderEntityNotFoundException;
import kitchenpos.domain.product.Product;
import kitchenpos.domain.product.ProductRepository;
import kitchenpos.utils.FixtureUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderServiceTest extends FixtureUtils {
    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderTableService orderTableService;

    @Autowired
    private MenuGroupRepository menuGroupRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private MenuRepository menuRepository;

    @DisplayName("1개 미만의 주문 항목으로 주문할 수 없다.")
    @Test
    void createOrderFailWithNotEnoughOrderLineItemsTest() {
        // given
        OrderTableResponse orderTableResponse = orderTableService.create(
                new OrderTableRequest(3, false));
        Long orderTableId = orderTableResponse.getId();
        OrderRequest orderRequest = new OrderRequest(orderTableId, new ArrayList<>());

        orderTableService.changeEmpty(orderTableId, new ChangeEmptyRequest(false));

        // when, then
        assertThatThrownBy(() -> orderService.create(orderRequest))
                .isInstanceOf(InvalidTryOrderException.class)
                .hasMessage("주문하기 위해서는 1개 이상의 주문 항목이 필요합니다.");
    }

    @DisplayName("메뉴에 없는 주문 항목으로 주문할 수 없다.")
    @Test
    void createOrderFailWithNotExistMenuTest() {
        // given
        Long orderTableId = 1L;
        Long notExistMenuId = 10000L;
        Long quantity = 1L;

        OrderLineItemRequest orderLineItemRequest = new OrderLineItemRequest(notExistMenuId, quantity);
        OrderRequest orderRequest = new OrderRequest(orderTableId, Collections.singletonList(orderLineItemRequest));

        // when, then
        assertThatThrownBy(() -> orderService.create(orderRequest))
                .isInstanceOf(MenuEntityNotFoundException.class)
                .hasMessage("존재하지 않는 메뉴가 있습니다.");
    }

    @DisplayName("존재하지 않는 주문테이블에서 주문할 수 없다.")
    @Test
    void createOrderFailWithNotExistOrderTableTest() {
        // given
        Long menuId = 1L;
        Long quantity = 1L;
        Long notExistOrderTableId = 100000L;

        OrderLineItemRequest orderLineItemRequest = new OrderLineItemRequest(menuId, quantity);
        OrderRequest orderRequest = new OrderRequest(notExistOrderTableId, Collections.singletonList(orderLineItemRequest));

        // when, then
        assertThatThrownBy(() -> orderService.create(orderRequest))
                .isInstanceOf(MenuEntityNotFoundException.class)
                .hasMessage("존재하지 않는 메뉴가 있습니다.");
    }

    @DisplayName("비어있는 주문 테이블에서 주문할 수 없다.")
    @Test
    void createOrderFailWithEmptyOrderTable() {
        // given
        MenuGroup menuGroup = menuGroupRepository.save(new MenuGroup("놀라운 메뉴 그룹"));
        Product product = productRepository.save(new Product("놀라운 상품", BigDecimal.ONE));
        Menu menu = menuRepository.save(Menu.of("놀라운 메뉴", BigDecimal.ZERO, menuGroup.getId(),
                Collections.singletonList(MenuProduct.of(product.getId(), 1L))));
        Long menuId = menu.getId();
        Long quantity = 1L;

        OrderTableResponse emptyTable = orderTableService.create(new OrderTableRequest(0, true));

        OrderLineItemRequest orderLineItemRequest = new OrderLineItemRequest(menuId, quantity);
        OrderRequest orderRequest = new OrderRequest(emptyTable.getId(), Collections.singletonList(orderLineItemRequest));

        // when, then
        assertThatThrownBy(() -> orderService.create(orderRequest))
                .isInstanceOf(InvalidTryOrderException.class)
                .hasMessage("비어있는 주문 테이블에서 주문할 수 없습니다.");
    }

    @DisplayName("주문할 수 있다.")
    @Test
    void createOrderTest() {
        // given
        MenuGroup menuGroup = menuGroupRepository.save(new MenuGroup("놀라운 메뉴 그룹"));
        Product product = productRepository.save(new Product("놀라운 상품", BigDecimal.ONE));
        Menu menu = menuRepository.save(Menu.of("놀라운 메뉴", BigDecimal.ZERO, menuGroup.getId(),
                Collections.singletonList(MenuProduct.of(product.getId(), 1L))));
        Long menuId = menu.getId();
        Long quantity = 1L;

        OrderTableResponse fullOrderTable = orderTableService.create(new OrderTableRequest(500, false));

        OrderLineItemRequest orderLineItemRequest = new OrderLineItemRequest(menuId, quantity);
        OrderRequest orderRequest = new OrderRequest(fullOrderTable.getId(), Collections.singletonList(orderLineItemRequest));

        // when
        OrderResponse orderResponse = orderService.create(orderRequest);

        // then
        assertThat(orderResponse.getOrderStatus()).isEqualTo(OrderStatus.COOKING.name());
        assertThat(orderResponse.getOrderTableId()).isEqualTo(fullOrderTable.getId());
        assertThat(orderResponse.getOrderedTime()).isNotNull();
        assertThat(orderResponse.getOrderLineItems()).hasSize(1);
    }

    @DisplayName("주문 목록을 조회할 수 있다.")
    @Test
    void getOrdersTest() {
        // given
        MenuGroup menuGroup = menuGroupRepository.save(new MenuGroup("놀라운 메뉴 그룹"));
        Product product = productRepository.save(new Product("놀라운 상품", BigDecimal.ONE));
        Menu menu = menuRepository.save(Menu.of("놀라운 메뉴", BigDecimal.ZERO, menuGroup.getId(),
                Collections.singletonList(MenuProduct.of(product.getId(), 1L))));
        Long menuId = menu.getId();
        Long quantity = 1L;

        OrderTableResponse fullOrderTable = orderTableService.create(new OrderTableRequest(500, false));

        OrderLineItemRequest orderLineItemRequest = new OrderLineItemRequest(menuId, quantity);
        OrderRequest orderRequest = new OrderRequest(fullOrderTable.getId(), Collections.singletonList(orderLineItemRequest));

        OrderResponse orderResponse = orderService.create(orderRequest);

        // when
        List<OrderResponse> orders = orderService.list();
        Stream<Long> ids = orders.stream()
                .map(OrderResponse::getId);

        // then
        assertThat(ids).contains(orderResponse.getId());
    }

    @DisplayName("존재하지 않는 주문의 주문 상태를 바꿀 수 없다.")
    @Test
    void changeOrderStatusFailWithNotExistOrderTest() {
        // given
        Long notExistOrder = 1L;

        OrderStatusChangeRequest changeOrderRequest = new OrderStatusChangeRequest(OrderStatus.MEAL.name());

        // when, then
        assertThatThrownBy(() -> orderService.changeOrderStatus(notExistOrder, changeOrderRequest))
                .isInstanceOf(OrderEntityNotFoundException.class)
                .hasMessage("존재하지 않는 주문입니다.");
    }

    @DisplayName("주문의 주문 상태를 바꿀 수 있다.")
    @Test
    void changeOrderStatusTest() {
        // given
        MenuGroup menuGroup = menuGroupRepository.save(new MenuGroup("놀라운 메뉴 그룹"));
        Product product = productRepository.save(new Product("놀라운 상품", BigDecimal.ONE));
        Menu menu = menuRepository.save(Menu.of("놀라운 메뉴", BigDecimal.ZERO, menuGroup.getId(),
                Collections.singletonList(MenuProduct.of(product.getId(), 1L))));
        Long menuId = menu.getId();
        Long quantity = 1L;

        OrderTableResponse fullOrderTable = orderTableService.create(new OrderTableRequest(500, false));

        OrderLineItemRequest orderLineItemRequest = new OrderLineItemRequest(menuId, quantity);
        OrderRequest orderRequest = new OrderRequest(fullOrderTable.getId(), Collections.singletonList(orderLineItemRequest));

        OrderResponse created = orderService.create(orderRequest);

        OrderStatusChangeRequest changeOrderRequest = new OrderStatusChangeRequest(OrderStatus.COMPLETION.name());

        // when
        OrderResponse orderResponse = orderService.changeOrderStatus(created.getId(), changeOrderRequest);

        // then
        assertThat(orderResponse.getOrderStatus()).isEqualTo(OrderStatus.COMPLETION.name());
    }

    @DisplayName("주문 상태가 계산 완료인 주문의 주문 상태를 바꿀 수 없다.")
    @Test
    void changeOrderStatusFailWithInvalidOrderStatus() {
        // given
        MenuGroup menuGroup = menuGroupRepository.save(new MenuGroup("놀라운 메뉴 그룹"));
        Product product = productRepository.save(new Product("놀라운 상품", BigDecimal.ONE));
        Menu menu = menuRepository.save(Menu.of("놀라운 메뉴", BigDecimal.ZERO, menuGroup.getId(),
                Collections.singletonList(MenuProduct.of(product.getId(), 1L))));
        Long menuId = menu.getId();
        Long quantity = 1L;

        OrderTableResponse fullOrderTable = orderTableService.create(new OrderTableRequest(500, false));

        OrderLineItemRequest orderLineItemRequest = new OrderLineItemRequest(menuId, quantity);
        OrderRequest orderRequest = new OrderRequest(fullOrderTable.getId(), Collections.singletonList(orderLineItemRequest));

        OrderResponse orderResponse = orderService.create(orderRequest);

        OrderStatusChangeRequest changeOrderRequest = new OrderStatusChangeRequest(OrderStatus.COMPLETION.name());

        orderService.changeOrderStatus(orderResponse.getId(), changeOrderRequest);

        // when, then
        assertThatThrownBy(() -> orderService.changeOrderStatus(orderResponse.getId(), changeOrderRequest))
                .isInstanceOf(InvalidTryChangeOrderStatusException.class)
                .hasMessage("계산 완료된 주문의 상태를 바꿀 수 없습니다.");
    }
}