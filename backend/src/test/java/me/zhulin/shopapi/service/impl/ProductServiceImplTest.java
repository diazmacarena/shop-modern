package me.zhulin.shopapi.service.impl;

import me.zhulin.shopapi.entity.ProductInfo;
import me.zhulin.shopapi.enums.ProductStatusEnum;
import me.zhulin.shopapi.exception.MyException;
import me.zhulin.shopapi.repository.ProductInfoRepository;
import me.zhulin.shopapi.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ProductServiceImplTest {

    @InjectMocks
    private ProductServiceImpl productService;

    @Mock
    private ProductInfoRepository productInfoRepository;

    @Mock
    private CategoryService categoryService;

    private ProductInfo productInfo;

    @BeforeEach
    public void setUp() {
        productInfo = new ProductInfo();
        productInfo.setProductId("1");
        productInfo.setProductStock(10);
        productInfo.setProductStatus(1);
    }

    @Test
    public void increaseStockTest() {
        when(productInfoRepository.findByProductId(productInfo.getProductId())).thenReturn(productInfo);

        productService.increaseStock("1", 10);

        Mockito.verify(productInfoRepository, Mockito.times(1)).save(productInfo);
    }

    @Test
    public void increaseStockExceptionTest() {
        assertThrows(MyException.class, () -> {
            productService.increaseStock("1", 10);
        });
    }

    @Test
    public void decreaseStockTest() {
        when(productInfoRepository.findByProductId(productInfo.getProductId())).thenReturn(productInfo);

        productService.decreaseStock("1", 9);

        Mockito.verify(productInfoRepository, Mockito.times(1)).save(productInfo);
    }

    @Test
    public void decreaseStockValueLesserEqualTest() {
        assertThrows(MyException.class, () -> {
            when(productInfoRepository.findByProductId(productInfo.getProductId())).thenReturn(productInfo);

            productService.decreaseStock("1", 10);
        });
    }

    @Test
    public void decreaseStockExceptionTest() {
        assertThrows(MyException.class, () -> {
            productService.decreaseStock("1", 10);
        });
    }

    @Test
    public void offSaleTest() {
        productInfo.setProductStatus(ProductStatusEnum.UP.getCode());

        when(productInfoRepository.findByProductId(productInfo.getProductId())).thenReturn(productInfo);

        productService.offSale("1");

        Mockito.verify(productInfoRepository, Mockito.times(1)).save(productInfo);
    }

    @Test
    public void offSaleStatusDownTest() {
        assertThrows(MyException.class, () -> {
            productInfo.setProductStatus(ProductStatusEnum.DOWN.getCode());

            when(productInfoRepository.findByProductId(productInfo.getProductId())).thenReturn(productInfo);

            productService.offSale("1");
        });
    }

    @Test
    public void offSaleProductNullTest() {
        assertThrows(MyException.class, () -> {
            when(productInfoRepository.findByProductId(productInfo.getProductId())).thenReturn(null);

            productService.offSale("1");
        });
    }

    @Test
    public void onSaleTest() {
        productInfo.setProductStatus(ProductStatusEnum.DOWN.getCode());

        when(productInfoRepository.findByProductId(productInfo.getProductId())).thenReturn(productInfo);

        productService.onSale("1");

        Mockito.verify(productInfoRepository, Mockito.times(1)).save(productInfo);
    }

    @Test
    public void onSaleStatusUpTest() {
        assertThrows(MyException.class, () -> {
            productInfo.setProductStatus(ProductStatusEnum.UP.getCode());

            when(productInfoRepository.findByProductId(productInfo.getProductId())).thenReturn(productInfo);

            productService.onSale("1");
        });
    }

    @Test
    public void onSaleProductNullTest() {
        assertThrows(MyException.class, () -> {
            when(productInfoRepository.findByProductId(productInfo.getProductId())).thenReturn(null);

            productService.offSale("1");
        });
    }

    @Test
    public void updateTest() {
        productService.update(productInfo);

        Mockito.verify(productInfoRepository, Mockito.times(1)).save(productInfo);
    }

    @Test
    public void updateProductStatusBiggerThenOneTest() {
        assertThrows(MyException.class, () -> {
            productInfo.setProductStatus(2);

            productService.update(productInfo);
        });
    }

    @Test
    public void deleteTest() {
        when(productInfoRepository.findByProductId(productInfo.getProductId())).thenReturn(productInfo);

        productService.delete("1");

        Mockito.verify(productInfoRepository, Mockito.times(1)).delete(productInfo);
    }

    @Test
    public void deleteProductNullTest() {
        assertThrows(MyException.class, () -> {
            productService.delete("1");
        });
    }
}
