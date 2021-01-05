package kitchenpos.application;

import kitchenpos.dao.OrderDao;
import kitchenpos.dao.OrderTableDao;
import kitchenpos.dao.TableGroupDao;
import kitchenpos.domain.OrderTable;
import kitchenpos.domain.TableGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class TableGroupServiceTest {
    private TableGroupService tableGroupService;

    @Mock
    private OrderDao orderDao;

    @Mock
    private OrderTableDao orderTableDao;

    @Mock
    private TableGroupDao tableGroupDao;

    private OrderTable emptyOrderTable1;
    private OrderTable emptyOrderTable2;
    private OrderTable fullAndGroupedOrderTable1;
    private OrderTable fullAndGroupedOrderTable2;
    private List<OrderTable> emptyOrderTables;
    private List<OrderTable> fullAndGroupedOrderTables;

    @BeforeEach
    void setup() {
        this.tableGroupService = new TableGroupService(orderDao, orderTableDao, tableGroupDao);

        emptyOrderTable1 = new OrderTable();
        emptyOrderTable1.setEmpty(true);

        emptyOrderTable2 = new OrderTable();
        emptyOrderTable2.setEmpty(true);

        fullAndGroupedOrderTable1 = new OrderTable();
        fullAndGroupedOrderTable1.setEmpty(false);
        fullAndGroupedOrderTable1.setTableGroupId(44L);
        fullAndGroupedOrderTable2 = new OrderTable();
        fullAndGroupedOrderTable2.setEmpty(false);
        fullAndGroupedOrderTable2.setTableGroupId(44L);

        emptyOrderTables = Arrays.asList(emptyOrderTable1, emptyOrderTable2);
        fullAndGroupedOrderTables = Arrays.asList(fullAndGroupedOrderTable1, fullAndGroupedOrderTable2);
    }

    @DisplayName("2개 이하의 주문테이블로 단체 지정할 수 없다.")
    @ParameterizedTest
    @MethodSource("tableGroupFailWithEmptyTableResource")
    void createTableGroupFailWithEmptyTable(List<OrderTable> orderTables) {
        // given
        TableGroup tableGroup = new TableGroup();
        tableGroup.setOrderTables(orderTables);

        // when, then
        assertThatThrownBy(() -> tableGroupService.create(tableGroup)).isInstanceOf(IllegalArgumentException.class);
    }
    public static Stream<Arguments> tableGroupFailWithEmptyTableResource() {
        return Stream.of(
                Arguments.of(new ArrayList<>()),
                Arguments.of(Collections.singletonList(new OrderTable()))
        );
    }

    @DisplayName("존재하지 않는 주문 테이블들로 단체 지정할 수 없다.")
    @Test
    void createTableGroupFailWithNotExistTableGroups() {
        // given
        TableGroup tableGroupWithNotExistOrderTables = new TableGroup();
        tableGroupWithNotExistOrderTables.setOrderTables(emptyOrderTables);

        // when, then
        assertThatThrownBy(() -> tableGroupService.create(tableGroupWithNotExistOrderTables))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("비어있지 않은 주문 테이블들로 단체 지정할 수 없다.")
    @Test
    void createTableGroupFailWithFullOrderTables() {
        // given
        TableGroup tableGroupWithFullOrderTables = new TableGroup();
        tableGroupWithFullOrderTables.setOrderTables(fullAndGroupedOrderTables);

        // when, then
        assertThatThrownBy(() -> tableGroupService.create(tableGroupWithFullOrderTables))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("이미 단체 지정된 주문 테이블들로 단체 지정할 수 없다.")
    @Test
    void createTableGroupFailWithAlreadyGrouped() {
        // given
        TableGroup tableGroupWithAlreadyGrouped = new TableGroup();
        tableGroupWithAlreadyGrouped.setOrderTables(fullAndGroupedOrderTables);

        // when, then
        assertThatThrownBy(() -> tableGroupService.create(tableGroupWithAlreadyGrouped))
                .isInstanceOf(IllegalArgumentException.class);
    }
}