package fu.cinema.service;

import fu.cinema.dto.request.FnBItemRequest;
import fu.cinema.dto.response.FnBItemResponse;
import fu.cinema.entity.FnBItem;
import fu.cinema.exception.DuplicateFnBItemException;
import fu.cinema.exception.FnBItemNotFoundException;
import fu.cinema.mapper.FnBItemMapper;
import fu.cinema.repository.FnBItemRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
@Service
@RequiredArgsConstructor
@Transactional
public class FnBItemServiceImpl implements FnBItemService {
    private final FnBItemRepository fnBItemRepository;
    private final FnBItemMapper fnBItemMapper;

    @Override
    @Transactional(readOnly = true)
    public List<FnBItemResponse> GetAllItems() {
        return fnBItemRepository.findAll()
                .stream()
                .map(fnBItemMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FnBItemResponse GetItemById(Long id) {
        FnBItem fnBItem = fnBItemRepository.findById(id)
                .orElseThrow(
                        ()-> new FnBItemNotFoundException(id)
                );
        return fnBItemMapper.toResponse(fnBItem);
    }

    @Override
    public FnBItemResponse createItem(FnBItemRequest fnBItemRequest) {
        if(fnBItemRepository.existsByNameIgnoreCase(
                fnBItemRequest.getName())){
            throw new DuplicateFnBItemException(fnBItemRequest.getName());
        };
        FnBItem fnBItem = fnBItemMapper.toEntity(fnBItemRequest);
        FnBItem fnBItemSaved = fnBItemRepository.save(fnBItem);
        return fnBItemMapper.toResponse(fnBItemSaved);
    }

    @Override
    public FnBItemResponse updateItem(Long id, FnBItemRequest fnBItemRequest) {
        FnBItem fnBItem = fnBItemRepository.findById(id)
                .orElseThrow(
                        ()-> new FnBItemNotFoundException(id)
                );
        if(!fnBItem.getName().equalsIgnoreCase(fnBItemRequest.getName())
        && fnBItemRepository.existsByNameIgnoreCase(
                fnBItemRequest.getName())){
                throw new DuplicateFnBItemException(fnBItemRequest.getName());
        }
        fnBItemMapper .update(fnBItem, fnBItemRequest);
        FnBItem updateItem = fnBItemRepository.save(fnBItem);

        return fnBItemMapper.toResponse(updateItem);
    }

    @Override
    public void deleteItem(Long id) {
        FnBItem fnBItem = fnBItemRepository.findById(id)
                .orElseThrow(
                        ()-> new FnBItemNotFoundException(id)
                );
        fnBItemRepository.delete(fnBItem);
    }

    @Override
    public FnBItemResponse changeAvailability(Long id, Boolean availability) {
        FnBItem fnBItem = fnBItemRepository.findById(id)
                .orElseThrow(
                        ()-> new FnBItemNotFoundException(id)
                );
        fnBItem.setIsAvailable(availability);
        FnBItem updateItem = fnBItemRepository.save(fnBItem);
        return fnBItemMapper.toResponse(updateItem);
    }
}
