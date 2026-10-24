package com.afs.common;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PageResult 单元测试
 *
 * 覆盖分页字段计算（pages 向上取整、size 为 0 的保护）、
 * of 静态工厂、hasNext/hasPrevious 边界及 getter/setter。
 */
class PageResultTest {

    @Test
    // pages 按 (total + size - 1) / size 向上取整
    void constructorComputesPagesWithCeilingDivision() {
        PageResult<String> result = new PageResult<>(List.of("a", "b", "c"), 10, 1, 3);

        assertThat(result.getRecords()).containsExactly("a", "b", "c");
        assertThat(result.getTotal()).isEqualTo(10);
        assertThat(result.getCurrent()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(3);
        assertThat(result.getPages()).isEqualTo(4);
    }

    @Test
    // size 为 0 时 pages 为 0，避免除零异常
    void constructorWithZeroSizeGivesZeroPages() {
        PageResult<String> result = new PageResult<>(List.of(), 10, 1, 0);

        assertThat(result.getPages()).isZero();
    }

    @Test
    // total 整除 size 时 pages 不多算一页
    void constructorWithExactDivisionGivesExactPages() {
        PageResult<String> result = new PageResult<>(List.of(), 9, 1, 3);

        assertThat(result.getPages()).isEqualTo(3);
    }

    @Test
    // of 静态工厂与构造器行为一致
    void ofFactoryCreatesEquivalentResult() {
        PageResult<Integer> result = PageResult.of(List.of(1, 2), 5, 2, 2);

        assertThat(result.getRecords()).containsExactly(1, 2);
        assertThat(result.getTotal()).isEqualTo(5);
        assertThat(result.getCurrent()).isEqualTo(2);
        assertThat(result.getSize()).isEqualTo(2);
        assertThat(result.getPages()).isEqualTo(3);
    }

    @Test
    // hasNext：当前页小于总页数时为 true，末页为 false
    void hasNextReflectsCurrentVsPages() {
        PageResult<String> middle = new PageResult<>(List.of("a"), 9, 2, 3);
        PageResult<String> last = new PageResult<>(List.of("a"), 9, 3, 3);

        assertThat(middle.hasNext()).isTrue();
        assertThat(last.hasNext()).isFalse();
    }

    @Test
    // hasPrevious：仅当当前页大于 1 时为 true
    void hasPreviousOnlyAfterFirstPage() {
        PageResult<String> first = new PageResult<>(List.of("a"), 9, 1, 3);
        PageResult<String> second = new PageResult<>(List.of("a"), 9, 2, 3);

        assertThat(first.hasPrevious()).isFalse();
        assertThat(second.hasPrevious()).isTrue();
    }

    @Test
    // 无参构造器配合 setter 可正常赋值读取
    void noArgsConstructorAndSettersWork() {
        PageResult<String> result = new PageResult<>();
        result.setRecords(List.of("x"));
        result.setTotal(1L);
        result.setCurrent(1L);
        result.setSize(10L);
        result.setPages(1L);

        assertThat(result.getRecords()).containsExactly("x");
        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getCurrent()).isEqualTo(1L);
        assertThat(result.getSize()).isEqualTo(10L);
        assertThat(result.getPages()).isEqualTo(1L);
    }
}
